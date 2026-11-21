package com.alananasss.kittytunewebsite.ui

data class AppChromeStrings(
    val navHome: String,
    val navFeatures: String,
    val navTogether: String,
    val navDownload: String,
    val changeLanguageA11y: String,
    val languagePanelTitle: String,
    val customizeThemeA11y: String,
    val closeA11y: String,
    val customizeTitle: String,
    val seedColorLabel: String,
    val colorBlue: String,
    val colorPurple: String,
    val colorTeal: String,
    val colorRed: String,
    val colorOrange: String,
    val colorPink: String,
    val colorIndigo: String,
    val colorCyan: String,
    val colorGreen: String,
    val colorGold: String,
    val paletteStyleLabel: String,
    val styleVibrant: String,
    val styleTonalSpot: String,
    val styleExpressive: String,
    val styleRainbow: String,
    val styleFruitSalad: String,
    val styleFidelity: String,
    val styleContent: String,
    val styleMonochrome: String,
    val styleNeutral: String,
) {
    fun colorName(key: String): String = when (key) {
        "Blue" -> colorBlue
        "Purple" -> colorPurple
        "Teal" -> colorTeal
        "Red" -> colorRed
        "Orange" -> colorOrange
        "Pink" -> colorPink
        "Indigo" -> colorIndigo
        "Cyan" -> colorCyan
        "Green" -> colorGreen
        "Gold" -> colorGold
        else -> key
    }

    fun paletteStyleName(key: String): String = when (key) {
        "Vibrant" -> styleVibrant
        "Tonal Spot" -> styleTonalSpot
        "Expressive" -> styleExpressive
        "Rainbow" -> styleRainbow
        "Fruit Salad" -> styleFruitSalad
        "Fidelity" -> styleFidelity
        "Content" -> styleContent
        "Monochrome" -> styleMonochrome
        "Neutral" -> styleNeutral
        else -> key
    }
}

fun appChromeStrings(locale: WebLocale): AppChromeStrings = when (locale) {
    WebLocale.EN -> AppChromeStringsEn
    WebLocale.FR -> AppChromeStringsFr
    WebLocale.DE -> AppChromeStringsDe
    WebLocale.HU -> AppChromeStringsHu
    WebLocale.PT_BR -> AppChromeStringsPtBr
    WebLocale.RU -> AppChromeStringsRu
    WebLocale.TR -> AppChromeStringsTr
    WebLocale.VI -> AppChromeStringsVi
}

// ═════════════════════════════════════════════════════════════════════════════
// ENGLISH (EN)
// ═════════════════════════════════════════════════════════════════════════════

private val AppChromeStringsEn = AppChromeStrings(
    navHome = "Home",
    navFeatures = "Features",
    navTogether = "Together",
    navDownload = "Download",
    changeLanguageA11y = "Change language",
    languagePanelTitle = "Language",
    customizeThemeA11y = "Customize theme",
    closeA11y = "Close",
    customizeTitle = "Customize",
    seedColorLabel = "Seed color",
    colorBlue = "Blue",
    colorPurple = "Purple",
    colorTeal = "Teal",
    colorRed = "Red",
    colorOrange = "Orange",
    colorPink = "Pink",
    colorIndigo = "Indigo",
    colorCyan = "Cyan",
    colorGreen = "Green",
    colorGold = "Gold",
    paletteStyleLabel = "Palette style",
    styleVibrant = "Vibrant",
    styleTonalSpot = "Tonal Spot",
    styleExpressive = "Expressive",
    styleRainbow = "Rainbow",
    styleFruitSalad = "Fruit Salad",
    styleFidelity = "Fidelity",
    styleContent = "Content",
    styleMonochrome = "Monochrome",
    styleNeutral = "Neutral",
)

// ═════════════════════════════════════════════════════════════════════════════
// FRENCH (FR)
// ═════════════════════════════════════════════════════════════════════════════

private val AppChromeStringsFr = AppChromeStrings(
    navHome = "Accueil",
    navFeatures = "Fonctions",
    navTogether = "Ensemble",
    navDownload = "Télécharger",
    changeLanguageA11y = "Changer de langue",
    languagePanelTitle = "Langue",
    customizeThemeA11y = "Personnaliser le thème",
    closeA11y = "Fermer",
    customizeTitle = "Personnaliser",
    seedColorLabel = "Couleur d’accent",
    colorBlue = "Bleu",
    colorPurple = "Violet",
    colorTeal = "Sarcelle",
    colorRed = "Rouge",
    colorOrange = "Orange",
    colorPink = "Rose",
    colorIndigo = "Indigo",
    colorCyan = "Cyan",
    colorGreen = "Vert",
    colorGold = "Or",
    paletteStyleLabel = "Style de palette",
    styleVibrant = "Vibrant",
    styleTonalSpot = "Tonalité douce",
    styleExpressive = "Expressif",
    styleRainbow = "Arc-en-ciel",
    styleFruitSalad = "Salade de fruits",
    styleFidelity = "Fidélité",
    styleContent = "Contenu",
    styleMonochrome = "Monochrome",
    styleNeutral = "Neutre",
)

// ═════════════════════════════════════════════════════════════════════════════
// GERMAN (DE)
// ═════════════════════════════════════════════════════════════════════════════

private val AppChromeStringsDe = AppChromeStrings(
    navHome = "Start",
    navFeatures = "Funktionen",
    navTogether = "Together",
    navDownload = "Download",
    changeLanguageA11y = "Sprache ändern",
    languagePanelTitle = "Sprache",
    customizeThemeA11y = "Design anpassen",
    closeA11y = "Schließen",
    customizeTitle = "Anpassen",
    seedColorLabel = "Akzentfarbe",
    colorBlue = "Blau",
    colorPurple = "Lila",
    colorTeal = "Petrol",
    colorRed = "Rot",
    colorOrange = "Orange",
    colorPink = "Pink",
    colorIndigo = "Indigo",
    colorCyan = "Cyan",
    colorGreen = "Grün",
    colorGold = "Gold",
    paletteStyleLabel = "Palettenstil",
    styleVibrant = "Lebendig",
    styleTonalSpot = "Tonale Balance",
    styleExpressive = "Expressiv",
    styleRainbow = "Regenbogen",
    styleFruitSalad = "Obstsalat",
    styleFidelity = "Originalgetreu",
    styleContent = "Inhalt",
    styleMonochrome = "Monochrom",
    styleNeutral = "Neutral",
)

// ═════════════════════════════════════════════════════════════════════════════
// HUNGARIAN (HU)
// ═════════════════════════════════════════════════════════════════════════════

private val AppChromeStringsHu = AppChromeStrings(
    navHome = "Főoldal",
    navFeatures = "Funkciók",
    navTogether = "Together",
    navDownload = "Letöltés",
    changeLanguageA11y = "Nyelv váltása",
    languagePanelTitle = "Nyelv",
    customizeThemeA11y = "Téma testreszabása",
    closeA11y = "Bezárás",
    customizeTitle = "Testreszabás",
    seedColorLabel = "Alapszín",
    colorBlue = "Kék",
    colorPurple = "Lila",
    colorTeal = "Pávakék",
    colorRed = "Piros",
    colorOrange = "Narancs",
    colorPink = "Rózsaszín",
    colorIndigo = "Indigó",
    colorCyan = "Cián",
    colorGreen = "Zöld",
    colorGold = "Arany",
    paletteStyleLabel = "Paletta stílusa",
    styleVibrant = "Élénk",
    styleTonalSpot = "Tónusos",
    styleExpressive = "Kifejező",
    styleRainbow = "Szivárvány",
    styleFruitSalad = "Gyümölcssaláta",
    styleFidelity = "Színhű",
    styleContent = "Tartalomhoz igazodó",
    styleMonochrome = "Monokróm",
    styleNeutral = "Semleges",
)

// ═════════════════════════════════════════════════════════════════════════════
// BRAZILIAN PORTUGUESE (PT_BR)
// ═════════════════════════════════════════════════════════════════════════════

private val AppChromeStringsPtBr = AppChromeStrings(
    navHome = "Início",
    navFeatures = "Recursos",
    navTogether = "Juntos",
    navDownload = "Baixar",
    changeLanguageA11y = "Alterar idioma",
    languagePanelTitle = "Idioma",
    customizeThemeA11y = "Personalizar tema",
    closeA11y = "Fechar",
    customizeTitle = "Personalizar",
    seedColorLabel = "Cor-base",
    colorBlue = "Azul",
    colorPurple = "Roxo",
    colorTeal = "Verde-azulado",
    colorRed = "Vermelho",
    colorOrange = "Laranja",
    colorPink = "Rosa",
    colorIndigo = "Índigo",
    colorCyan = "Ciano",
    colorGreen = "Verde",
    colorGold = "Dourado",
    paletteStyleLabel = "Estilo da paleta",
    styleVibrant = "Vibrante",
    styleTonalSpot = "Ponto tonal",
    styleExpressive = "Expressivo",
    styleRainbow = "Arco-íris",
    styleFruitSalad = "Salada de frutas",
    styleFidelity = "Fidelidade",
    styleContent = "Conteúdo",
    styleMonochrome = "Monocromático",
    styleNeutral = "Neutro",
)

// ═════════════════════════════════════════════════════════════════════════════
// RUSSIAN (RU)
// ═════════════════════════════════════════════════════════════════════════════

private val AppChromeStringsRu = AppChromeStrings(
    navHome = "Главная",
    navFeatures = "Функции",
    navTogether = "Together",
    navDownload = "Скачать",
    changeLanguageA11y = "Сменить язык",
    languagePanelTitle = "Язык",
    customizeThemeA11y = "Настроить тему",
    closeA11y = "Закрыть",
    customizeTitle = "Оформление",
    seedColorLabel = "Базовый цвет",
    colorBlue = "Синий",
    colorPurple = "Фиолетовый",
    colorTeal = "Бирюзовый",
    colorRed = "Красный",
    colorOrange = "Оранжевый",
    colorPink = "Розовый",
    colorIndigo = "Индиго",
    colorCyan = "Циановый",
    colorGreen = "Зелёный",
    colorGold = "Золотой",
    paletteStyleLabel = "Стиль палитры",
    styleVibrant = "Яркий",
    styleTonalSpot = "Тональный акцент",
    styleExpressive = "Экспрессивный",
    styleRainbow = "Радуга",
    styleFruitSalad = "Фруктовый микс",
    styleFidelity = "Точный",
    styleContent = "Контентный",
    styleMonochrome = "Монохромный",
    styleNeutral = "Нейтральный",
)

// ═════════════════════════════════════════════════════════════════════════════
// TURKISH (TR)
// ═════════════════════════════════════════════════════════════════════════════

private val AppChromeStringsTr = AppChromeStrings(
    navHome = "Ana Sayfa",
    navFeatures = "Özellikler",
    navTogether = "Birlikte",
    navDownload = "İndir",
    changeLanguageA11y = "Dili değiştir",
    languagePanelTitle = "Dil",
    customizeThemeA11y = "Temayı özelleştir",
    closeA11y = "Kapat",
    customizeTitle = "Özelleştir",
    seedColorLabel = "Temel renk",
    colorBlue = "Mavi",
    colorPurple = "Mor",
    colorTeal = "Petrol yeşili",
    colorRed = "Kırmızı",
    colorOrange = "Turuncu",
    colorPink = "Pembe",
    colorIndigo = "Çivit mavisi",
    colorCyan = "Camgöbeği",
    colorGreen = "Yeşil",
    colorGold = "Altın",
    paletteStyleLabel = "Palet stili",
    styleVibrant = "Canlı",
    styleTonalSpot = "Tonal Vurgu",
    styleExpressive = "Etkileyici",
    styleRainbow = "Gökkuşağı",
    styleFruitSalad = "Meyve Salatası",
    styleFidelity = "Aslına Uygun",
    styleContent = "İçerik Odaklı",
    styleMonochrome = "Tek Renk",
    styleNeutral = "Nötr",
)

// ═════════════════════════════════════════════════════════════════════════════
// VIETNAMESE (VI)
// ═════════════════════════════════════════════════════════════════════════════

private val AppChromeStringsVi = AppChromeStrings(
    navHome = "Trang chủ",
    navFeatures = "Tính năng",
    navTogether = "Together",
    navDownload = "Tải xuống",
    changeLanguageA11y = "Đổi ngôn ngữ",
    languagePanelTitle = "Ngôn ngữ",
    customizeThemeA11y = "Tùy chỉnh giao diện",
    closeA11y = "Đóng",
    customizeTitle = "Tùy chỉnh",
    seedColorLabel = "Màu chủ đạo",
    colorBlue = "Xanh dương",
    colorPurple = "Tím",
    colorTeal = "Xanh mòng két",
    colorRed = "Đỏ",
    colorOrange = "Cam",
    colorPink = "Hồng",
    colorIndigo = "Chàm",
    colorCyan = "Xanh lơ",
    colorGreen = "Xanh lá",
    colorGold = "Vàng kim",
    paletteStyleLabel = "Kiểu bảng màu",
    styleVibrant = "Rực rỡ",
    styleTonalSpot = "Điểm nhấn sắc độ",
    styleExpressive = "Biểu cảm",
    styleRainbow = "Cầu vồng",
    styleFruitSalad = "Trái cây nhiệt đới",
    styleFidelity = "Trung thực",
    styleContent = "Theo nội dung",
    styleMonochrome = "Đơn sắc",
    styleNeutral = "Trung tính",
)

