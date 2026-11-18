package com.alananasss.kittytunewebsite.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FeaturesPage() {
    val locale = LocalWebLocale.current
    val strings = featuresStrings(locale)

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
                    text = strings.pageTitle,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = strings.pageSubtitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FeatureIsland(
                title = strings.lyricsIslandTitle,
                description = strings.lyricsIslandDesc
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(48.dp)) {
                    MediaCard(
                        asset = MediaAsset(
                            "ket4eki-music-wordbyword.mp4",
                            strings.lyricsWordTitle,
                            strings.lyricsWordCaption,
                            MediaFit.Contain
                        ),
                        aspectRatio = 16f / 10f,
                        innerPadding = 48.dp,
                        imageCssFilter = "drop-shadow(0px 16px 32px rgba(0,0,0,0.5))"
                    )
                    AdaptivePair(
                        first = {
                            MediaCard(
                                asset = MediaAsset(
                                    "translation.mp4",
                                    strings.lyricsTranslationTitle,
                                    strings.lyricsTranslationCaption,
                                    MediaFit.Contain
                                ),
                                aspectRatio = 16f / 10f,
                                innerPadding = 32.dp,
                                imageCssFilter = "drop-shadow(0px 16px 32px rgba(0,0,0,0.5))",
                                modifier = it
                            )
                        },
                        second = {
                            MediaCard(
                                asset = MediaAsset(
                                    "romanization.mp4",
                                    strings.lyricsRomanizationTitle,
                                    strings.lyricsRomanizationCaption,
                                    MediaFit.Contain
                                ),
                                aspectRatio = 16f / 10f,
                                innerPadding = 32.dp,
                                imageCssFilter = "drop-shadow(0px 16px 32px rgba(0,0,0,0.5))",
                                modifier = it
                            )
                        }
                    )
                }
            }

            FeatureIsland(
                title = strings.scIslandTitle,
                description = strings.scIslandDesc
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    AdaptivePair(
                        spacing = 24.dp,
                        first = {
                            SyncCard(
                                strings.scLikesTitle,
                                strings.scLikesDesc,
                                Icons.Rounded.Favorite,
                                it
                            )
                        },
                        second = {
                            SyncCard(
                                strings.scHistoryTitle,
                                strings.scHistoryDesc,
                                Icons.Rounded.History,
                                it
                            )
                        }
                    )
                    AdaptivePair(
                        spacing = 24.dp,
                        first = {
                            SyncCard(
                                strings.scPlaylistsTitle,
                                strings.scPlaylistsDesc,
                                Icons.Rounded.QueueMusic,
                                it
                            )
                        },
                        second = {
                            SyncCard(
                                strings.scOAuthTitle,
                                strings.scOAuthDesc,
                                Icons.Rounded.Security,
                                it
                            )
                        }
                    )
                }
            }

            FeatureIsland(
                title = strings.discoveryIslandTitle,
                description = strings.discoveryIslandDesc
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(64.dp)) {
                    AdaptivePair(
                        first = {
                            MediaCard(
                                MediaAsset(
                                    "shazam.mp4",
                                    strings.shazamVideoTitle,
                                    strings.shazamVideoCaption,
                                    MediaFit.Cover
                                ),
                                modifier = it
                            )
                        },
                        second = {
                            MediaCard(
                                asset = MediaAsset(
                                    "shazamresult.png",
                                    strings.shazamResultTitle,
                                    strings.shazamResultCaption,
                                    MediaFit.Contain
                                ),
                                modifier = it,
                                innerPadding = 32.dp,
                                imageCssFilter = "drop-shadow(0px 12px 24px rgba(0,0,0,0.5))"
                            )
                        }
                    )

                    AdaptivePair(
                        first = { childMod ->
                            SectionHeader(
                                title = strings.socialContextTitle,
                                description = strings.socialContextDesc,
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
                title = strings.pixelIslandTitle,
                description = strings.pixelIslandDesc
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(64.dp)) {
                    AdaptivePair(
                        first = { childMod ->
                            SectionHeader(
                                title = strings.dynamicColorsTitle,
                                description = strings.dynamicColorsDesc,
                                modifier = childMod
                            )
                        },
                        second = { childMod ->
                            MediaCard(
                                asset = MediaAsset(
                                    "personalisationpart1.mp4",
                                    strings.monetMediaTitle,
                                    strings.monetMediaCaption
                                ),
                                modifier = childMod
                            )
                        }
                    )

                    AdaptivePair(
                        first = { childMod ->
                            MediaCard(
                                asset = MediaAsset(
                                    "personalisationpart2.mp4",
                                    strings.googleSansMediaTitle,
                                    strings.googleSansMediaCaption
                                ),
                                modifier = childMod
                            )
                        },
                        second = { childMod ->
                            SectionHeader(
                                title = strings.variableTypoTitle,
                                description = strings.variableTypoDesc,
                                modifier = childMod
                            )
                        }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        SectionHeader(
                            title = strings.amoledSectionTitle,
                            description = strings.amoledSectionDesc,
                            centered = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ComparisonBoard(
                            assetLeft = MediaAsset("amoled.png", strings.amoledLeftTitle, ""),
                            assetRight = MediaAsset("normal (no amoled).png", strings.amoledRightTitle, "")
                        )
                    }
                }
            }

            FeatureIsland(
                title = strings.libraryIslandTitle,
                description = strings.libraryIslandDesc
            ) {
                AdaptivePair(
                    first = {
                        MediaCard(
                            MediaAsset(
                                "playlist1.mp4",
                                strings.playlistTableTitle,
                                strings.playlistTableCaption
                            ),
                            modifier = it,
                            aspectRatio = 16f / 10f
                        )
                    },
                    second = {
                        MediaCard(
                            asset = MediaAsset(
                                "storage.png",
                                strings.storageCacheTitle,
                                strings.storageCacheCaption,
                                fit = MediaFit.Contain
                            ),
                            modifier = it,
                            aspectRatio = 16f / 10f,
                            innerPadding = 32.dp,
                            imageCssFilter = "drop-shadow(0px 12px 24px rgba(0,0,0,0.5))"
                        )
                    }
                )
            }

            FeatureIsland(
                title = strings.osIslandTitle,
                description = strings.osIslandDesc
            ) {
                AdaptiveGrid(
                    items = listOf(
                        MediaAsset(
                            "windowspanelmusic.png",
                            strings.osWindowsTitle,
                            strings.osWindowsCaption,
                            MediaFit.Contain
                        ),
                        MediaAsset(
                            "kdeplasma.png",
                            strings.osKdeTitle,
                            strings.osKdeCaption,
                            MediaFit.Contain
                        ),
                        MediaAsset(
                            fileName = "musicpanelmpris.png",
                            title = strings.osHyprlandTitle,
                            caption = strings.osHyprlandCaption,
                            fit = MediaFit.Contain,
                            linkUrl = "https://github.com/end-4/dots-hyprland",
                            linkText = strings.osHyprlandLinkText
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
