"""
KittyTune website performance benchmark.

Measures frame times (requestAnimationFrame deltas) in headless Chromium for:
  load        first 12 s after navigation (the splash hides the heavy start-up frames)
  idle        1.5 s with nothing happening
  tabs 700ms  switching top tabs at a normal pace
  tabs 120ms  switching tabs as fast as possible
  colors      picking 5 seed colours in the theme panel

Usage (from the repo root):
  ./gradlew wasmJsBrowserDevelopmentExecutableDistribution
  python3 tools/perf/serve.py build/dist/wasmJs/developmentExecutable &
  pip install playwright && playwright install chromium
  python3 tools/perf/bench.py /tmp/bench            # numbers
  python3 tools/perf/bench.py /tmp/bench --profile  # + CPU profile (inclusive time per function)
  python3 tools/perf/bench.py /tmp/bench --profile --trace=dynamicColorScheme  # call stacks

Headless Chromium renders with SwiftShader (CPU), so absolute numbers are pessimistic; compare runs.
Note: in --profile output, each scenario's profile is printed *before* its summary line.
"""
import sys, asyncio, statistics, json, collections
from playwright.async_api import async_playwright
TABS=[(604,41),(804,41),(702,41),(505,41)]
THEME=(1231,41)
REC = "window.__ft=[];window.__last=0;(function l(t){if(window.__last)window.__ft.push(t-window.__last);window.__last=t;requestAnimationFrame(l)})(performance.now());"
PROFILE = '--profile' in sys.argv
TRACE = next((a.split('=')[1] for a in sys.argv if a.startswith('--trace=')), None)
FILTER = ['alananasss','createJsError','Intl','Locale','shapes','Morph','materialkolor','Composer.','recompose','measure','draw','Layout','Text','Paragraph']
def stats(name, f):
    if not f: print(name,'no frames'); return
    f=sorted(f); p95=f[int(len(f)*0.95)-1]
    print(f"{name:14s} frames={len(f):4d} median={statistics.median(f):6.1f} p95={p95:6.1f} max={max(f):6.1f} >33ms={sum(1 for x in f if x>33):3d} >50ms={sum(1 for x in f if x>50):3d} jank_ms={sum(x-16.7 for x in f if x>20):7.0f}")
async def profiled(cdp, coro):
    if PROFILE:
        await cdp.send('Profiler.enable'); await cdp.send('Profiler.setSamplingInterval', {'interval': 200}); await cdp.send('Profiler.start')
    await coro
    if PROFILE:
        prof = (await cdp.send('Profiler.stop'))['profile']
        nodes = {n['id']: n for n in prof['nodes']}
        cnt = collections.Counter(prof['samples'])
        selft = collections.Counter()
        for nid, c in cnt.items():
            cf = nodes[nid]['callFrame']; selft[cf['functionName'] or '(anon) '+cf['url'][-30:]] += c
        tot = sum(cnt.values())
        parent = {}
        for n in prof['nodes']:
            for ch in n.get('children', []): parent[ch] = n['id']
        incl = collections.Counter()
        for nid, c in cnt.items():
            seen = set(); cur = nid
            while cur is not None:
                fn = nodes[cur]['callFrame']['functionName']
                if fn and fn not in seen: seen.add(fn); incl[fn] += c
                cur = parent.get(cur)
        if TRACE:
            stacks = collections.Counter()
            for nid, c in cnt.items():
                chain=[]; cur=nid
                while cur is not None:
                    fn = nodes[cur]['callFrame']['functionName']
                    if fn: chain.append(fn)
                    cur = parent.get(cur)
                if any(TRACE in f for f in chain):
                    keep=[f.split('.')[-1] if 'alananasss' not in f else f for f in reversed(chain) if ('alananasss' in f or 'materialkolor' in f or 'Composer' in f or 'draw' in f.lower() or 'Effect' in f or 'COROUTINE' in f or 'invoke' in f)]
                    stacks[' > '.join([k for k in keep if 'materialkolor' not in k and k not in ('invoke','getTone','getHct','getArgb')][-16:])] += c
            for st, c in stacks.most_common(4): print('   STACK', c, st[:900])
        busy = tot - selft.get('(idle)', 0)
        print(f"   busy {100*busy/tot:.1f}% of samples")
        shown = 0
        for fn, c in incl.most_common(400):
            if any(k in fn for k in FILTER):
                print(f"   {100*c/tot:5.1f}%  {fn[:120]}"); shown += 1
                if shown >= 40: break
async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(args=['--use-gl=angle','--use-angle=swiftshader','--enable-unsafe-swiftshader'])
        ctx = await b.new_context(viewport={'width':1440,'height':900}, locale='en-US')
        pg = await ctx.new_page()
        cdp = await ctx.new_cdp_session(pg)
        await pg.add_init_script("try{localStorage.setItem('web_lang','en');localStorage.setItem('kt_theme_dark_v3','0')}catch(e){}")
        await pg.add_init_script(REC)
        await pg.goto('http://127.0.0.1:8765/')
        await pg.wait_for_timeout(12000)
        stats('load(0-12s)', await pg.evaluate("window.__ft.splice(0)"))
        await pg.wait_for_timeout(1500); stats('idle', await pg.evaluate("window.__ft.splice(0)"))
        await pg.screenshot(path=sys.argv[1]+'_top.png')
        tabs = TABS
        async def slow():
            for i in range(8):
                x,y = tabs[i%4]; await pg.mouse.click(x,y); await pg.wait_for_timeout(700)
        await profiled(cdp, slow()); stats('tabs 700ms', await pg.evaluate("window.__ft.splice(0)"))
        async def fast():
            for i in range(16):
                x,y = tabs[i%4]; await pg.mouse.click(x,y); await pg.wait_for_timeout(120)
            await pg.wait_for_timeout(1500)
        await profiled(cdp, fast()); stats('tabs 120ms', await pg.evaluate("window.__ft.splice(0)"))
        await pg.mouse.click(505,41); await pg.wait_for_timeout(2500)
        await pg.mouse.click(THEME[0],THEME[1]); await pg.wait_for_timeout(1200); await pg.screenshot(path=sys.argv[1]+'_panel.png'); await pg.evaluate("window.__ft.splice(0)")
        async def colors():
            for x in [1186,1244,1302,1360,1128]:
                await pg.mouse.click(x,312); await pg.wait_for_timeout(900)
        await profiled(cdp, colors()); stats('colors', await pg.evaluate("window.__ft.splice(0)"))
        await pg.screenshot(path=sys.argv[1]+'_end.png')
        await b.close()
asyncio.run(main())
