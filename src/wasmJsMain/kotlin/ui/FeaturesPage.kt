package com.alananasss.kittytunewebsite.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FeaturesPage() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isMobile = maxWidth < 600.dp
        val horizontalPad = if (isMobile) 16.dp else 48.dp

        DesktopScrollableColumn(
            modifier = Modifier.fillMaxSize(),
            columnModifier = Modifier
                .padding(horizontal = horizontalPad)
                .padding(top = if (isMobile) 32.dp else 80.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(if (isMobile) 80.dp else 160.dp)
        ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "All features,\nin detail.",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Discover the power and flexibility of the KittyTune ecosystem, without the fluff.",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FeatureIsland(
            title = "Lyrics without borders.",
            description = "Understand and sing your favorite songs. KittyTune handles word-by-word highlighting, phonetic transcription, and real-time translation."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(48.dp)) {
                MediaCard(
                    asset = MediaAsset("ket4eki-music-wordbyword.mp4", "Word by word", "Native support for enriched LRC format.", MediaFit.Contain),
                    aspectRatio = 16f/10f,
                    innerPadding = 48.dp,
                    imageCssFilter = "drop-shadow(0px 16px 32px rgba(0,0,0,0.5))"
                )
                AdaptivePair(
                    first = { 
                        MediaCard(
                            asset = MediaAsset("translation.mp4", "Integrated translation", "Understand the meaning without leaving the player.", MediaFit.Contain), 
                            aspectRatio = 16f/10f, 
                            innerPadding = 32.dp, 
                            imageCssFilter = "drop-shadow(0px 16px 32px rgba(0,0,0,0.5))", 
                            modifier = it 
                        ) 
                    },
                    second = { 
                        MediaCard(
                            asset = MediaAsset("romanization.mp4", "CJK Romanization", "Ideal for Japanese and Korean.", MediaFit.Contain), 
                            aspectRatio = 16f/10f, 
                            innerPadding = 32.dp, 
                            imageCssFilter = "drop-shadow(0px 16px 32px rgba(0,0,0,0.5))", 
                            modifier = it 
                        ) 
                    }
                )
            }
        }

        FeatureIsland(
            title = "Your SoundCloud account,\nperfectly synced.",
            description = "Connect via the official authentication (OAuth PKCE). KittyTune doesn't just play your music; it keeps your profile updated in real-time with SoundCloud servers."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                AdaptivePair(
                    spacing = 24.dp,
                    first = { SyncCard("Likes & Reposts", "Every like or repost made in the app is instantly pushed to your official account, and vice versa.", Icons.Rounded.Favorite, it) },
                    second = { SyncCard("Listening History", "Tracks played on KittyTune automatically appear in your SoundCloud 'Recently Played' history.", Icons.Rounded.History, it) }
                )
                AdaptivePair(
                    spacing = 24.dp,
                    first = { SyncCard("Playlist Management", "Create, edit, reorder, and delete your playlists. Your local changes directly modify your online sets.", Icons.Rounded.QueueMusic, it) },
                    second = { SyncCard("OAuth Security", "The login flow opens the official SoundCloud domain. Your credentials are never seen or intercepted by KittyTune.", Icons.Rounded.Security, it) }
                )
            }
        }

        FeatureIsland(
            title = "Context, Discovery, and Social Proof.",
            description = "The integrated Shazam algorithm recognizes the music playing around you. Once on a track's profile, immediately see who liked it thanks to the SoundCloud API."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(64.dp)) {
                AdaptivePair(
                    first = { MediaCard(MediaAsset("shazam.mp4", "Native Recognition", "Identify any track.", MediaFit.Cover), modifier = it) },
                    second = { 
                        MediaCard(
                            asset = MediaAsset("shazamresult.png", "Instant Result", "HD cover and direct actions.", MediaFit.Contain), 
                            modifier = it,
                            innerPadding = 32.dp,
                            imageCssFilter = "drop-shadow(0px 12px 24px rgba(0,0,0,0.5))"
                        ) 
                    }
                )
                
                AdaptivePair(
                    first = { childMod ->
                        SectionHeader(
                            title = "Social Context",
                            description = "Comments and likes synchronized live from SoundCloud. The interface displays instant social proof without cluttering the screen.",
                            modifier = childMod
                        )
                    },
                    second = { childMod ->
                        MediaCard(
                            asset = MediaAsset(
                                fileName = "socialproof.png", 
                                title = "",
                                caption = "",
                                fit = MediaFit.Contain
                            ),
                            aspectRatio = 1f,
                            innerPadding = 32.dp,
                            imageCssFilter = "drop-shadow(0px 16px 32px rgba(0,0,0,0.6))", 
                            modifier = childMod
                        )
                    }
                )
            }
        }

        FeatureIsland(
            title = "The Google Pixel aesthetics, pushed to the limit.",
            description = "Material You generates palettes from your images. The Google Sans Flex typography adapts, and the AMOLED mode turns off your pixels for maximum battery savings."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(64.dp)) {
                AdaptivePair(
                    first = { childMod ->
                        SectionHeader(
                            title = "Dynamic Colors",
                            description = "KittyTune extracts colors from your wallpaper or album covers. Choose from dozens of palette styles.",
                            modifier = childMod
                        )
                    },
                    second = { childMod ->
                        MediaCard(
                            asset = MediaAsset("personalisationpart1.mp4", "Monet Engine", "Real-time theme generation."),
                            modifier = childMod
                        )
                    }
                )

                AdaptivePair(
                    first = { childMod ->
                        MediaCard(
                            asset = MediaAsset("personalisationpart2.mp4", "Google Sans Flex", "Adjust the weight, roundness, and width."),
                            modifier = childMod
                        )
                    },
                    second = { childMod ->
                        SectionHeader(
                            title = "Variable Typography",
                            description = "Push Material Design 3 even further by customizing the system font axes directly from the application.",
                            modifier = childMod
                        )
                    }
                )

                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    SectionHeader(
                        title = "Pure AMOLED Black vs Tonal Gray",
                        description = "For battery purists, a switch transforms Material gray surfaces into absolute black (#000000).",
                        centered = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ComparisonBoard(
                        assetLeft = MediaAsset("amoled.png", "AMOLED Mode (Pure Black)", ""),
                        assetRight = MediaAsset("normal (no amoled).png", "Standard Dark Mode", "")
                    )
                }
            }
        }

        FeatureIsland(
            title = "A library built for control.",
            description = "Sort, download, and manage thousands of local and remote tracks. Easily purge unwanted data from the storage manager."
        ) {
            AdaptivePair(
                first = { MediaCard(MediaAsset("playlist1.mp4", "Playlist Table", "Fast manipulation and smart sorting of Desktop data."), modifier = it, aspectRatio = 16f/10f) },
                second = { 
                    MediaCard(
                        asset = MediaAsset("storage.png", "Storage and Cache", "Selective cleanup and disk space control.", fit = MediaFit.Contain), 
                        modifier = it, 
                        aspectRatio = 16f/10f,
                        innerPadding = 32.dp,
                        imageCssFilter = "drop-shadow(0px 12px 24px rgba(0,0,0,0.5))"
                    ) 
                }
            )
        }

        FeatureIsland(
            title = "Native integrations to your Operating System.",
            description = "MPRIS on Linux (KDE, GNOME) and SMTC on Windows. Your media keys and OS control panels drive KittyTune naturally."
        ) {
            AdaptiveGrid(
                items = listOf(
                    MediaAsset("windowspanelmusic.png", "Windows SMTC", "Native 10/11 control panel.", MediaFit.Contain),
                    MediaAsset("kdeplasma.png", "KDE Plasma", "Seamless integration to the Linux desktop.", MediaFit.Contain),
                    MediaAsset(
                        fileName = "musicpanelmpris.png", 
                        title = "Hyprland / MPRIS", 
                        caption = "Perfectly integrated with community widgets.", 
                        fit = MediaFit.Contain,
                        linkUrl = "https://github.com/end-4/dots-hyprland",
                        linkText = "→ Widget via end-4/dots-hyprland"
                    )
                ),
                aspectRatio = 1f,
                innerPadding = 32.dp,
                imageCssFilter = "drop-shadow(0px 12px 24px rgba(0,0,0,0.5))"
            )
        }
    }
}
}

@Composable
private fun SyncCard(title: String, description: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(32.dp), 
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape, 
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onPrimaryContainer, 
                    modifier = Modifier.padding(16.dp).size(32.dp)
                )
            }
            Text(
                title, 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                description, 
                style = MaterialTheme.typography.bodyLarge, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                lineHeight = 24.sp
            )
        }
    }
}
