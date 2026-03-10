# DjoudinisIPPlayer-v2 - Comprehensive Feature Checklist

> **Analysis Date:** 2026-03-10  
> **App Type:** Android IPTV Player with TV/Fire TV Support

---

## 📊 Data Layer Features

### Entities (Room Database)

| Feature | Status | Details |
|---------|--------|---------|
| **PlaylistEntity** | ✅ | Supports Xtream Codes & M3U playlists |
| **CategoryEntity** | ✅ | Live/VOD/Series categories with selection state |
| **ChannelEntity** | ✅ | Live TV with EPG mapping, favorites, catch-up support |
| **VodEntity** | ✅ | Movies with metadata (cast, director, genre, rating, TMDB ID) |
| **SeriesEntity** | ✅ | TV series metadata with cover art |
| **EpisodeEntity** | ✅ | Individual episodes with season/episode numbers |
| **EpgProgramEntity** | ✅ | EPG programs with start/stop times, catch-up URLs |
| **WatchProgressEntity** | ✅ | Resume playback + Trakt sync state |
| **PlayerConfig** | ✅ | Buffer settings, User-Agent, decoding preferences |

### DAO Operations

| Feature | Status | Details |
|---------|--------|---------|
| **CategoryDao** | ✅ | CRUD, observe by type, selection management |
| **ChannelDao** | ✅ | Favorites, recently watched, search, channel zapping (prev/next) |
| **VodDao** | ✅ | By category, favorites, search, recently added |
| **SeriesDao** | ✅ | By category, search |
| **EpisodeDao** | ✅ | By series/season, next episode lookup, hasNextEpisode check |
| **EpgProgramDao** | ✅ | By channel/time range, current/next program, batch insert |
| **PlaylistDao** | ✅ | CRUD, active playlist management, account info updates |
| **WatchProgressDao** | ✅ | Upsert, continue watching, history, Trakt sync status |

### API Integrations

| Feature | Status | Details |
|---------|--------|---------|
| **Xtream Codes API** | ✅ | Full API: auth, categories, streams, series info, EPG |
| **Trakt.tv API** | ✅ | OAuth, history sync, watchlist, scrobble (start/pause/stop) |
| **M3U Parsing** | ✅ | Chunked streaming parser for large playlists |
| **XMLTV Parsing** | ✅ | Batch EPG parsing with OOM protection |

---

## 🎮 ViewModel Features

| ViewModel | State Management | Operations |
|-----------|------------------|------------|
| **DashboardViewModel** | ✅ Active playlist, continue watching, favorites, recently watched, sync progress | Sync playlist/EPG, cancel sync |
| **PlayerViewModel** | ✅ UI state (title, URL, EPG, controls), playback position, resume dialog | Play/Pause, seek, channel zapping (UP/DOWN), next episode, fallback URLs, progress tracking |
| **ContentListViewModel** | ✅ Categories by type, channels/VOD/series, view mode (list/grid/large), sort mode, search | Category selection, inline search, toggle favorites, cycle view/sort modes |
| **OnboardingViewModel** | ✅ Login state, category filter state, sync progress | Xtream/M3U login, category selection, sync categories/streams |
| **EpgViewModel** | ✅ EPG data (channels + programs) | Load EPG for active playlist |
| **SettingsViewModel** | ✅ Player config, auto sync, Trakt status, theme | Update all settings, sync now, disconnect Trakt, clear history |
| **VodDetailViewModel** | ✅ VOD details, resume position | Load VOD with watch progress |
| **SeriesDetailViewModel** | ✅ Series details, seasons, episodes, episode progress, loading state | Fetch episodes from API, load episodes by season |

---

## 📱 UI Screens

### Mobile Screens (`presentation/ui/mobile`)

| Screen | Status | Features |
|--------|--------|----------|
| **OnboardingScreen** | ✅ | Welcome, Xtream/M3U selection |
| **LoginXtreamScreen** | ✅ | Server URL, username, password input |
| **LoginM3uScreen** | ✅ | M3U URL input |
| **CategoryFilterScreen** | ✅ | 3-step wizard (Live → VOD → Series), select/deselect categories |
| **DashboardScreen** | ✅ | Continue watching, favorites, recently watched, sync progress, navigation tiles |
| **LiveCategoriesScreen** | ✅ | Split-pane: categories list + channels grid |
| **VodCategoriesScreen** | ✅ | Split-pane: categories list + movies grid |
| **SeriesCategoriesScreen** | ✅ | Split-pane: categories list + series grid |
| **VodDetailScreen** | ✅ | Movie poster, metadata, plot, play button, resume support |
| **SeriesDetailScreen** | ✅ | Series info, season selector, episode list with progress |
| **PlayerScreen** | ✅ | ExoPlayer with controls, EPG overlay, resume dialog |
| **EpgGridScreen** | ✅ | Grid view of channels with current/next programs |
| **MultiViewScreen** | ✅ | Picture-in-Picture / split-screen (mobile only) |
| **SearchScreen** | ✅ | Global search across channels/VOD/series |
| **SettingsScreen** | ✅ | Player settings, sync settings, Trakt, theme |

### TV Screens (`presentation/ui/tv`)

| Screen | Status | Features |
|--------|--------|----------|
| **TvOnboardingScreen** | ✅ | TV-optimized welcome with D-Pad navigation |
| **TvLoginXtreamScreen** | ✅ | TV-optimized form with focus handling |
| **TvLoginM3uScreen** | ✅ | TV-optimized M3U input |
| **TvCategoryFilterScreen** | ✅ | TV-optimized category selection |
| **TvDashboardScreen** | ✅ | Leanback-style dashboard with focusable cards |
| **TvCategoryListScreen** | ✅ | TV-optimized category navigation |
| **TvChannelListScreen** | ✅ | Channel grid with EPG preview |
| **TvVodDetailScreen** | ✅ | TV-optimized movie details with play focus |
| **TvPlayerOverlay** | ✅ | D-Pad controls, channel zapping (UP/DOWN), EPG info, next episode |

---

## 🎬 Player Features

### Core Playback

| Feature | Status | Details |
|---------|--------|---------|
| **ExoPlayer Integration** | ✅ | Media3 ExoPlayer with custom factory |
| **Live TV (HLS/TS)** | ✅ | Channel streaming with fallback URLs |
| **VOD Playback** | ✅ | Movies with resume support |
| **Episode Playback** | ✅ | TV episodes with next episode support |
| **Resume Playback** | ✅ | Auto-save progress every 10s, resume dialog |
| **Multiple Formats** | ✅ | TS, M3U8, MPEGTS, MP4, MKV |

### Player Controls

| Feature | Status | Details |
|---------|--------|---------|
| **Play/Pause** | ✅ | Basic playback control |
| **Seek Forward/Back** | ✅ | +/- 10 seconds |
| **Channel Zapping** | ✅ | D-PAD UP/DOWN for prev/next channel |
| **Next Episode** | ✅ | Auto-play next episode for series |
| **Controls Visibility** | ✅ | Auto-hide/show, toggle with D-PAD |
| **EPG Overlay** | ✅ | Current/Next program display |
| **Stream Fallback** | ✅ | Auto-retry with different extensions (ts/m3u8/mpegts) |

### Advanced Player Features

| Feature | Status | Details |
|---------|--------|---------|
| **User-Agent Spoofing** | ✅ | Configurable UA (VLC, Chrome, Smart TV) |
| **Buffer Configuration** | ✅ | Min/max buffer, playback buffer, rebuffer settings |
| **Hardware Decoding** | ✅ | Toggle hardware/software decoding |
| **Tunneled Playback** | ✅ | Android TV low-latency mode |
| **Auto Frame Rate** | ✅ | Match TV refresh rate to content (AFR) |
| **Multi-View Support** | ✅ | Reduced buffer players for PiP/split-screen |
| **Audio Focus** | ✅ | Handle audio becoming noisy |
| **Wake Lock** | ✅ | Network wake mode for streaming |

---

## ⚙️ Settings/Preferences

### Player Settings

| Setting | Status | Options |
|---------|--------|---------|
| **User-Agent** | ✅ | VLC (default), Chrome, Smart TV, Custom |
| **Buffer Sizes** | ✅ | Min/Max buffer, playback buffer, rebuffer |
| **Software Decoding** | ✅ | Toggle prefer software over hardware |
| **Tunneled Playback** | ✅ | Enable/disable for Android TV |

### Sync Settings

| Setting | Status | Details |
|---------|--------|---------|
| **Auto Sync** | ✅ | Enable/disable automatic sync |
| **Playlist Sync Interval** | ✅ | Default 6 hours, configurable |
| **EPG Sync Interval** | ✅ | Default 12 hours, configurable |
| **Manual Sync** | ✅ | Sync now buttons for playlist/EPG |

### Trakt.tv Integration

| Feature | Status | Details |
|---------|--------|---------|
| **OAuth Authentication** | ✅ | Token-based auth with refresh |
| **Watch History Sync** | ✅ | Sync completed movies/episodes |
| **Scrobbling** | ✅ | Real-time progress to Trakt |
| **Disconnect** | ✅ | Clear Trakt tokens |

### UI Settings

| Setting | Status | Options |
|---------|--------|---------|
| **Theme** | ✅ | Dark (default), Light, System |
| **EPG Hours Visible** | ✅ | Configurable hours in EPG grid |

### Data Management

| Feature | Status | Details |
|---------|--------|---------|
| **Clear Watch History** | ✅ | Delete all watch progress |
| **Reset Settings** | ✅ | Restore default configuration |

---

## 🧭 Navigation

### Routes (Type-Safe Navigation)

| Route | Parameters | Purpose |
|-------|------------|---------|
| **Onboarding** | - | Initial setup |
| **LoginXtream** | - | Xtream login form |
| **LoginM3u** | - | M3U URL input |
| **CategoryFilter** | `playlistId` | Smart onboarding category selection |
| **Dashboard** | - | Main hub |
| **LiveCategories** | - | Browse live TV |
| **VodCategories** | - | Browse movies |
| **SeriesCategories** | - | Browse series |
| **VodDetail** | `contentType`, `contentId` | Movie details |
| **SeriesDetail** | `seriesId` | Series details & episodes |
| **Player** | `contentType`, `contentId` | Video playback |
| **EpgGrid** | - | EPG timeline view |
| **MultiView** | - | Picture-in-Picture (mobile) |
| **Search** | - | Global search |
| **Settings** | - | App settings |

### Navigation Flows

| Flow | Status | Steps |
|------|--------|-------|
| **First Launch** | ✅ | Onboarding → Login → Category Filter → Dashboard |
| **Add Playlist** | ✅ | Dashboard → Login → Category Filter |
| **Watch Live TV** | ✅ | Dashboard → Live Categories → Player |
| **Watch VOD** | ✅ | Dashboard → VOD Categories → Detail → Player |
| **Watch Series** | ✅ | Dashboard → Series Categories → Detail → Player |
| **Resume Watching** | ✅ | Dashboard → Continue Watching → Player |

---

## 🔄 Background Sync (WorkManager)

| Worker | Status | Function |
|--------|--------|----------|
| **PlaylistSyncWorker** | ✅ | Periodic playlist sync (default 6h) |
| **EpgSyncWorker** | ✅ | Periodic EPG sync (default 12h) + cleanup |
| **TraktSyncWorker** | ✅ | Sync watch history to Trakt.tv |
| **SyncScheduler** | ✅ | Schedule/cancel all sync operations |

---

## 📺 TV/Fire TV Specific Features

| Feature | Status | Details |
|---------|--------|---------|
| **D-Pad Navigation** | ✅ | Full remote control support |
| **Focus Management** | ✅ | Visual focus indicators on all TV screens |
| **TvPlayerOverlay** | ✅ | Remote-optimized controls with key handling |
| **Channel Zapping** | ✅ | UP/DOWN for channel switching |
| **Leanback Support** | ✅ | Android TV launcher integration ready |
| **Tunneled Playback** | ✅ | Low-latency playback mode |
| **Auto Frame Rate** | ✅ | Match display to content frame rate |

---

## 🚀 Advanced Features

### Catch-up / Timeshift

| Feature | Status | Details |
|---------|--------|---------|
| **Catch-up Support** | ✅ | Default, Append, Shift, Flussonic formats |
| **Archive Window** | ✅ | Configurable days per channel |
| **EPG Catch-up URLs** | ✅ | Build catch-up URLs from EPG data |

### EPG Features

| Feature | Status | Details |
|---------|--------|---------|
| **XMLTV Parsing** | ✅ | Standard XMLTV format support |
| **Current Program** | ✅ | Now playing info |
| **Next Program** | ✅ | Upcoming show info |
| **Program Grid** | ✅ | Time-based grid view |
| **Auto Cleanup** | ✅ | Remove expired programs |

### Smart Onboarding

| Feature | Status | Details |
|---------|--------|---------|
| **Two-Phase Sync** | ✅ | Categories first (fast), streams second |
| **Category Filtering** | ✅ | Select only wanted categories |
| **Progress Tracking** | ✅ | Real-time sync progress UI |
| **Selection Persistence** | ✅ | Remember selections on re-sync |

---

## ⚠️ Potential Missing Features / Areas to Check

### Features to Verify

| Feature | Status | Notes |
|---------|--------|-------|
| **Picture-in-Picture (PiP)** | ⚠️ | MultiViewScreen exists but verify PiP mode |
| **Subtitles/CC** | ⚠️ | Basic subtitle support in ExoPlayer but no explicit UI |
| **Audio Track Selection** | ⚠️ | ExoPlayer supports it but verify UI implementation |
| **Video Quality Selection** | ⚠️ | ABR for HLS but manual quality selector? |
| **Parental Controls** | ❌ | Not found in codebase |
| **Recording/DVR** | ❌ | Not implemented |
| **Download/Offline** | ❌ | Not implemented |
| **Multiple Playlists** | ⚠️ | CRUD exists but verify UI for switching |
| **Account Info Screen** | ⚠️ | Route defined but verify full implementation |
| **Player Settings Screen** | ⚠️ | Route defined but verify full implementation |
| **Search History** | ❌ | Not implemented |
| **Favorites Sync** | ❌ | Local only, no cloud sync |
| **Watchlist Management** | ⚠️ | Trakt watchlist fetch exists but verify UI |
| **Live TV Preview** | ❌ | No channel preview thumbnails |
| **Notifications** | ⚠️ | Verify if new content notifications exist |
| **Analytics/Crash Reporting** | ❌ | Not implemented |
| **Chromecast** | ❌ | Not implemented |
| **Voice Search** | ⚠️ | TV search may support voice input |

### TV-Specific Checks

| Feature | Status | Notes |
|---------|--------|-------|
| **Recommendations Row** | ❌ | Android TV home screen recommendations |
| **Search Integration** | ⚠️ | Global search provider integration |
| **Channel Banner** | ❌ | Android TV live channels app integration |
| **Gamepad Support** | ⚠️ | Verify game controller navigation |
| **Screensaver Prevention** | ⚠️ | Verify during playback |

---

## 📋 Summary

### ✅ Implemented (Strong)

1. **Complete IPTV Core:** Xtream Codes & M3U playlist support
2. **Full Content Types:** Live TV, VOD, Series with episodes
3. **EPG Support:** XMLTV parsing with current/next program
4. **Player Features:** Resume, catch-up, fallback URLs, channel zapping
5. **TV Optimization:** D-Pad controls, focus management, TV-specific UI
6. **Trakt Integration:** History sync, scrobbling
7. **Background Sync:** WorkManager for playlist/EPG sync
8. **Smart Onboarding:** Two-phase sync with category filtering
9. **Auto Frame Rate:** Match display refresh to content
10. **Multi-View:** Split-screen/PiP support (mobile)

### ⚠️ Partial / Needs Verification

1. Subtitle/CC support and UI
2. Audio track selection UI
3. Manual video quality selection
4. Search history
5. Watchlist UI for Trakt
6. Full account info screen
7. Complete player settings screen

### ❌ Not Implemented

1. Parental controls
2. Recording/DVR
3. Download/Offline playback
4. Cloud favorites sync
5. Chromecast support
6. Android TV recommendations row
7. Analytics/Crash reporting
8. Channel banner (Android TV Live Channels)

---

**Legend:**
- ✅ = Fully implemented
- ⚠️ = Partially implemented or needs verification
- ❌ = Not implemented
