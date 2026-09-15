# App Builder

This is a ready-to-go Offline First Compose Multiplatform starter kit built to help you spin up full-stack apps fast.
It bundles a Compose Multiplatform frontend for Android, iOS, Desktop, and Web right alongside a built-in backend.
Everything is pre-wired so you can skip the boilerplate and go straight to shipping for every platform from a single codebase.

## What It Means

* A persistent, fault-tolerant [offline job queue](#offline-engine) that keeps working without connectivity and resumes on its own, with conflict-aware [delta sync](#delta-sync) on top.
* A [push suite](#push-notifications) over FCM, WebSocket and SSE presented as one stream, with a server that [scales to multiple instances](#multi-instance-scaling) without sticky sessions.
* Accounts, sessions and per-entity permissions, a configurable [design system](#design), [translations](#translations), [feature flags and remote configs](#feature-flags--remote-configs), and a [test harness](#multiplatform-test-harness) that runs Compose UI and a full Ktor server from common code.

It is licensed **AGPL-3.0**, which is worth knowing before you build on it: derivative works have to be distributed under the same license with source available, and that applies to network services too, not just distributed binaries. 
If that doesn't suit you, the project still works as a reference architecture. See [License](#license).

# Shameless Plug

For me, writing code is like writing poetry. It’s about crafting logic for humans, not just making the machine understand, and expressing complex ideas with rhythm, economy, and elegance.
This project is a labor of love, and I would deeply appreciate any feedback you might have.

Open an issue or start a discussion at [github.com/vdribeiro/AppBuilder](https://github.com/vdribeiro/AppBuilder).

# Getting Started

Everything below works with zero configuration: no Firebase project, no Postgres, no env vars, no `local.properties`.

**Prerequisites.** Gradle comes from the wrapper, but the toolchain does not: install **JDK 21** before anything else, since no toolchain resolver is configured and Gradle will not download one for you. 
Beyond that, only per-target tooling: the Android SDK for Android, and Xcode plus CocoaPods (`brew install cocoapods`) for iOS. Desktop and Web need nothing else.

1. **Start the server**: `./gradlew :server:run`. This task injects a full dev environment, including an admin account. Boots on `http://localhost:8080`.
2. **Run the client** (dev mode targets `http://localhost:8080` automatically, via `URL.DEV_HOST`):
    * **Desktop**: `./gradlew :clientApp:run`
    * **Web**: `./gradlew :clientApp:wasmJsBrowserDevelopmentRun`
    * **Android**: `./gradlew :clientApp:installDebug`
    * **iOS**: `cd iosApp && pod install`, then open `iosApp.xcworkspace` in Xcode
3. **Log in** with the seeded admin credentials.

The default admin account credentials are `admin`/`admin`, and it is seeded with all permissions, which is what lets it create further accounts from inside the app.

A few things are pre-wired so that the above needs no setup, and are worth knowing because they are also the things that break if you change one half without the other:

* The server's port falls back to the same `URL.DEV_PORT` constant the client's `DEV_HOST` is built from, so both sides agree on `8080` by construction rather than by coincidence.
* The web dev server's `8010` is in the server's development CORS allowlist. Change the port and API calls from the browser start failing CORS.
* Both CocoaPods podspecs are committed, so `pod install` resolves on a fresh clone without running a Gradle task first. It will also pull the `FirebaseCore` and `FirebaseMessaging` pods, so the first run needs a network connection.
* `.run/` holds shared IDE run configurations for all four clients (`androidApp`, `desktopApp`, `webApp`, `iosApp`) and for the Android, desktop, web, iOS and server test suites, so the commands above have a click-to-run equivalent.

# Table of Contents

* [Architecture](#architecture)
    * [Supported Platforms](#supported-platforms)
    * [Platform Source Sets](#platform-source-sets)
    * [Main Tech Stack](#main-tech-stack)
    * [Modules](#modules)
    * [Package Responsibilities](#package-responsibilities)
    * [Modules Structure](#modules-structure)
        * [Shared](#shared)
        * [Shared Test](#shared-test)
        * [Design](#design)
        * [App Core](#app-core)
        * [Client App](#client-app)
        * [Server](#server)
* [Development Workflow](#development-workflow)
    * [Environment Setup](#environment-setup)
    * [Development vs Production](#development-vs-production)
    * [API Routes](#api-routes)
    * [Push Notifications](#push-notifications)
        * [Multi-Instance Scaling](#multi-instance-scaling)
        * [Client Connection Lifecycle](#client-connection-lifecycle)
        * [Firebase Setup](#firebase-setup)
    * [Feature Creation](#feature-creation)
        * [Shared](#shared-1)
        * [Design](#design-1)
        * [Client App](#client-app-1)
        * [Server](#server-1)
    * [Registries](#registries)
        * [Assets](#assets)
        * [Persisted Files](#persisted-files)
        * [Domain & Network](#domain--network)
        * [Client](#client)
        * [Server](#server-2)
        * [Tests](#tests)
    * [UI Specifics](#ui-specifics)
        * [Navigation](#navigation)
    * [Translations](#translations)
        * [JSON Structure](#json-structure)
        * [Usage](#usage)
    * [Context Management](#context-management)
    * [Testing](#testing)
    * [Platform Specific Implementations](#platform-specific-implementations)
    * [Deployment & Distribution](#deployment--distribution)
* [Offline Engine](#offline-engine)
    * [Job](#job)
    * [Conflict Policies](#conflict-policies)
    * [Chain System](#chain-system)
    * [Job Lifecycle End-to-End](#job-lifecycle-end-to-end)
    * [Network Awareness](#network-awareness)
    * [Disabling the Scheduler](#disabling-the-scheduler)
    * [Clock Trust](#clock-trust)
    * [Retry & Failure Handling](#retry--failure-handling)
    * [Zombie Recovery](#zombie-recovery)
    * [Delta Sync](#delta-sync)
        * [Collection Sync](#collection-sync)
        * [Push-Triggered Entity Sync](#push-triggered-entity-sync)
* [Noteworthy Code](#noteworthy-code)
    * [LazyData](#lazydata)
    * [Multiplatform Test Harness](#multiplatform-test-harness)
    * [Mock Http Engine](#mock-http-engine)
    * [Audio Player](#audio-player)
    * [Camera](#camera)
    * [NFC](#nfc)
    * [Device Location](#device-location)
    * [Cryptography](#cryptography)
    * [Persisted Storage](#persisted-storage)
    * [Clock Trust Service](#clock-trust-service)
    * [Push Suite](#push-suite)
    * [Server Instance Scaling](#server-instance-scaling)
    * [Offline Job Scheduler](#offline-job-scheduler)
    * [Http Stack](#http-stack)
    * [Feature Flags & Remote Configs](#feature-flags--remote-configs)
    * [Telemetry Hub](#telemetry-hub)
    * [MVI Store](#mvi-store)
    * [Router & Scene Strategies](#router--scene-strategies)
    * [Translation Pipeline](#translation-pipeline)
    * [Permission Manager](#permission-manager)
    * [Honourable Mentions](#honourable-mentions)
* [FAQ](#faq)
    * [How do I set up the project from scratch?](#how-do-i-set-up-the-project-from-scratch)
    * [Do I need Postgres or Firebase to develop locally?](#do-i-need-postgres-or-firebase-to-develop-locally)
    * [Why is dependency injection done manually instead of using a DI framework?](#why-is-dependency-injection-done-manually-instead-of-using-a-di-framework)
    * [Can I use the client without the bundled server?](#can-i-use-the-client-without-the-bundled-server)
    * [Can I add a Splash, Home and Error Screens?](#can-i-add-a-splash-home-and-error-screens)
    * [Do I need user and session management?](#do-i-need-user-and-session-management)
    * [Should I share domain models directly with the server, or use separate DTOs?](#should-i-share-domain-models-directly-with-the-server-or-use-separate-dtos)
    * [How do I handle platform-specific permissions?](#how-do-i-handle-platform-specific-permissions)
    * [How do I add offline support for a new operation?](#how-do-i-add-offline-support-for-a-new-operation)
    * [How are data sync conflicts handled between the client and server?](#how-are-data-sync-conflicts-handled-between-the-client-and-server)
    * [How do I turn a feature off, or roll one out gradually?](#how-do-i-turn-a-feature-off-or-roll-one-out-gradually)
    * [How do I add a new translation or language?](#how-do-i-add-a-new-translation-or-language)
    * [How do I run the tests, and why does the build fail on coverage?](#how-do-i-run-the-tests-and-why-does-the-build-fail-on-coverage)
    * [Can I drop down to native platform code if KMP lacks a feature?](#can-i-drop-down-to-native-platform-code-if-kmp-lacks-a-feature)
    * [Why does Desktop never use FCM for push notifications?](#why-does-desktop-never-use-fcm-for-push-notifications)
    * [Why do Android and iOS close their WebSocket/SSE connections in the background instead of keeping them alive?](#why-do-android-and-ios-close-their-websocketsse-connections-in-the-background-instead-of-keeping-them-alive)
    * [Why doesn't the server retry a failed WebSocket send by falling back to FCM for that same notification?](#why-doesnt-the-server-retry-a-failed-websocket-send-by-falling-back-to-fcm-for-that-same-notification)
    * [Why are WebSocket connection tickets and connection presence stored in the database instead of an in-memory map?](#why-are-websocket-connection-tickets-and-connection-presence-stored-in-the-database-instead-of-an-in-memory-map)
    * [Why Postgres LISTEN/NOTIFY instead of Redis or a message broker for cross-instance signaling?](#why-postgres-listennotify-instead-of-redis-or-a-message-broker-for-cross-instance-signaling)
    * [Where do I change the production server host?](#where-do-i-change-the-production-server-host)
    * [Do you use AI?](#do-you-use-ai)
* [License](#license)

# Architecture

**Kotlin Multiplatform** project following a **Layered Clean Architecture** designed around a **Unidirectional Dependency Flow**.

## Supported Platforms

* Android
* iOS
* Windows
* macOS
* Linux
* Web

## Platform Source Sets

| Source Set    | Platform        |
|---------------|-----------------|
| `commonMain`  | All             |
| `androidMain` | Android API 26+ |
| `appleMain`   | iOS 16+         |
| `desktopMain` | JVM 21          |
| `webMain`     | WASM-JS         |
| `server`      | JVM 21          |

## Main Tech Stack

* UI: [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
* Client Database: [SQLDelight](https://sqldelight.github.io/sqldelight)
* Server Database: [Exposed](https://www.jetbrains.com/exposed/)
* Networking: [Ktor](https://ktor.io/)
* Code Coverage: [Kover](https://github.com/Kotlin/kotlinx-kover)

## Modules

* `shared`: Code that is reused across all target platforms (Client and Server).
* `sharedTest`: Test fixtures, base test cases, and mocks reused by every other module's test suite.
* `design`: Design System where all UI components live.
* `designShowcase`: An app for previewing and experimenting with the `design` module.
* `appCore`: Reusable Compose Multiplatform app infrastructure used by `clientApp`.
* `clientApp`: The end-user client app for Android, iOS, Desktop, and Web.
* `server`: The backend for the application.
* `build-logic`: Build logic with app identity, versions, target configuration, and build-time code generation. Included as a composite build rather than a project dependency.

Production dependencies form a single downward graph, with `shared` at the bottom and nothing ever pointing back up:

```
shared ──┬── design ── appCore ── clientApp
         └── server
```

`sharedTest` is the one module that does not sit in that graph, because it is consumed from test source sets rather than production ones. Every other module pulls it in from its own `commonTest`.

The packages across all modules obey the same structure.

## Package Responsibilities

Each package has a specific isolated responsibility, and dependencies always point in a single direction down the stack.

* **core**: Business logic agnostic implementations. It is the foundation and cannot depend on any other layer.
* **data**: Responsible for data persistence and retrieval. Provides infrastructure for Network, Database and Storage. It can only depend on **core**.
* **domain**: Business workflows and rules. It can depend on **data** and **core**.
* **ui**: User-facing presentation layer. It can depend on **domain**, **data** and **core**.
* **test**: Test utilities like annotations and fake data shared across test source sets. It can depend on all layers, **domain**, **data**, **core**, **ui**.

Not every module has every package. The lists below are the union across modules.

#### core

* **config**: Remote configs with containers for feature flags and configurations.
* **flow**: Coroutine dispatchers and suspending primitives.
* **locale**: Localization and date/time formatting.
* **platform**: OS-specific APIs.
* **media**: Media implementations.
* **devicelocation**: Device location capture.
* **nfc**: NFC reading and writing.
* **security**: Cryptography and UUID utilities.
* **telemetry**: Logging and crash reporting.

#### data

* **device**: Device specific code.
* **http**: `Ktor` configuration and network logic.
* **serializer**: JSON parsing and serialization.
* **database**: `SQLDelight` or `Exposed` implementations, tables and drivers.
* **resource**: Resource index.
* **signal**: Cross-instance signaling for push delivery.
* **storage**: File system access.
* **translation**: Translation related tools.

#### domain

* **clock**: Clock services.
* **deeplink**: Deep link dispatch.
* **push**: Broadcast and Push services.
* **nfc**: NFC tag interpretation.
* **scheduler**: Scheduler implementation.
* **translation**: Translation service.
* **gateway**: Per-feature use cases, repositories and their gateway implementation.
* **usecase**: Implementation of specific business workflows.
* **permission**: Per-user permission tracking.
* **route**: Defines the routing endpoints for the server.

Also holds the domain models themselves at the root of the package.

#### ui

* **core**: UI core components like `Text` or `Button`, that provide the building blocks for composite components.
* **component**: UI composite components.
* **lifecycle**: Platform-aware lifecycle observers.
* **push**: Push composables.
* **media**: Media composables.
* **devicelocation**: Device location composables.
* **nfc**: NFC composables.
* **modifier**: Modifier extensions.
* **navigation**: Routing logic.
* **permission**: System permissions management.
* **screen**: UI entry points built with components. Each screen is also a sub-package containing the composable and respective store for state management, all co-located.

#### test

Testing suites, fake data and annotations.

## Modules Structure

### Shared

#### core

* **config**
    * `ClientFlags`: Client feature flag container.
    * `ClientConfigs`: Client config container.
    * `ServerFlags`: Server feature flag container.
    * `ServerConfigs`: Server config container.
* **flow**
    * `Dispatcher`: Provides `Main`, `Default` and `IO` coroutine dispatchers as mutable properties so tests can substitute their own implementations.
    * `LazyData`: Mutex-protected suspend-once-cache-forever lazy loader: the `suspend` equivalent of `lazy {}`.
* **locale**: Localization and date/time formatting.
    * `DateTime`: Utilities for time, e.g. getting the current time in UTC/ISO8601.
* **platform**: OS-specific APIs.
    * `Platform`: Provides a snapshot of the current execution environment with a sealed interface that defines the possible platforms that the application can run on.
    * `Loop`: Suspending, non-blocking polling loop helper.
    * `System`: Methods to retrieve environment variables and system properties.
* **security**: Cryptography and UUID utilities.
    * `Uuid`: UUID generation via the best available platform algorithm.
* **telemetry**: Logging and crash reporting.
    * `TelemetryEngine`: Interface implemented by every logging backend.
    * `Telemetry`: Hub for dispatching telemetry data, including logs, error reports, and user feedback, by iterating over pluggable `TelemetryEngine` implementations.
    * `Console`: An in-memory circular log-buffer `TelemetryEngine`.
    * `SentryLogger`: Sentry integration and implementation of `TelemetryEngine`.

#### data

* **http**: `Ktor` configuration and network logic.
    * `Header`: Sealed class of all header keys.
    * `Query`: Sealed class of all query parameter keys.
    * `URL`: Sealed class of all remote endpoints, serving as a centralized registry for API routing.
    * `HttpRequest`: Represents an HTTP request with associated metadata.
* **serializer**: JSON parsing and serialization.
    * `Json`: Helpers for encoding/decoding.

#### domain

Contains all the domain models, each `@Serializable` and sent over the wire as-is, with no separate DTO layer.

* `EntityType`: Enumerates every syncable entity. The offline engine, the permission map and the push payloads all key off it.
* `Permission`: The `READ`/`WRITE` access levels paired with an `EntityType` to form a user's permission map.
* `User`, `UserCredentials`, `UserSession`, `RegistrationForm`, `Authentication`, `BearerToken`: Accounts, login input and issued tokens.
* `DeviceToken`: A registered FCM token for a device.
* `PushPayload`: The push envelope delivered over WebSocket, SSE and FCM alike.
* `Registry`: A recorded request audit entry.
* `DeviceLocation`: A captured location fix.
* `Translation`: A single `languageIso`/`key`/`value` localization triple.

Also contains sample feature entities and its user joins.

#### test

Annotations used to exclude tests from coverage reports.

#### root level

* `KotlinExtensions`: Common Kotlin language extensions.
* `AndroidExtensions`: Android specific extensions.
* `AppleExtensions`: Apple specific extensions.

### Shared Test

#### core

* **telemetry**: Logging and crash reporting.
    * `MockLogger`: A mock implementation of the `TelemetryEngine` interface.

#### data

* **http**
    * `TestEngine`: A Ktor `MockEngine` plus response helpers.

#### ui

* **lifecycle**: Platform-aware lifecycle observers.
    * `OverridableLifecycleOwner`: A `LifecycleOwner` that can be manually set to a specific state.

#### test

* `FakeData`: A collection of fake data for testing.
* `SharedTestCase`: Class that provides the shared hermetic testing environment, providing helpers to run unit and UI tests.
* `PlatformTestCase`: Class that extends `SharedTestCase` for platform-specific test setup.
* `Extensions`: Testing helper extensions.

### Design

#### data

* **http**: `Ktor` configuration and network logic.
    * `createNetworkEngine`: Platform `HttpClientEngine` factory used to build the design module's own independent `HttpClient`, used by `ImageLoader` for image loading.
* **translation**: Translation related tools.
    * `TranslationCache`: A cache dedicated to translations.

#### ui

* `Color`: Defines `LocalColorScheme` for light and dark themes.
* `Typography`: Defines `LocalTypography` for the text fonts.
* `Shapes`: Defines `LocalShapes` for the shape system.
* `Translations`: Composable helpers to read from `TranslationCache`. Provides `LocalTranslationState`.
* `Theme`: Main app theme definition that uses the previously described `CompositionLocal`s. Also provides `LocalSplitScreen` and registers the image loader via `RegisterImageLoader()`.
* `Preview`: Wrapper composable that applies the application theme for accurate rendering in previews.
* **core**: Building blocks, carrying no knowledge of the app's domain, organized by category.
* **component**: Composite components assembled from the core blocks, which do know the domain types they render.

The split is what keeps the module reusable: `core` could be lifted into an unrelated project unchanged, while `component` is where the app's shapes appear. 
Neither holds a state or talks to a gateway. A component takes its data and its callbacks as parameters, which is why the whole module is previewable.

### App Core

#### core

* **media**: Media implementations.
    * `AudioPlayer`: Manages playlist deduplication and shuffling.
    * `Camera`: Manages photo capture and video recording.
* **devicelocation**: Device location capture.
    * `DeviceLocationProvider`: An update loop that keeps the platform's location cache warm, so locations can be fetched by pooling its cache passively. 
    * `DeviceLocation`: The captured location fix representation.
* **nfc**: NFC reading and writing.
    * `NfcController`: NFC reader/writer controller.
    * `NfcTag`: Nfc Tag representation.
    * `TagProvider`: Provider to collect the tags read by the platforms.
* **locale**: Localization and date/time formatting.
    * `Language`: App default language and other language listings.
    * `Locale`: Methods to get the current system language, locale-aware datetime formatting, and a `Flow` that emits on locale changes.
    * `Clock`: System clock changes observer.
* **platform**: OS-specific APIs.
    * `Development`: Development mode flag.
    * `AppPath`: Declares `appDataPath` and `appCachePath`, the absolute paths to the application's data and cache directories.
* **security**: Cryptography and UUID utilities.
    * `Cryptography`: Cryptography methods like encrypt/decrypt and hashing.
* **telemetry**: Logging and crash reporting.
    * `PlatformLogger`: A `TelemetryEngine` implementation for native system console outputs.

#### domain

* **clock**: Clock services.
    * `ClockService`: Tracks whether the device clock can be trusted. Implemented by `ClockManager`.
* **deeplink**: Deep link dispatch.
    * `DeepLink`: Sealed interface of deep link intents, delivered on a `Flow` and collected by the client's navigation layer to rebuild the backstack.
* **push**: Broadcast and Push services.
    * `BroadcastService`: Broadcast service that listens for push notifications. Implemented by `BroadcastManager`.
    * `PushService`: Push service that listens for user push notifications. Implemented by `PushManager`.
    * `PushProvider`: Provider to collect the push token, deduplicate and dispatch push payloads, trigger native notifications, and turn a notification tap into a `DeepLink`.
    * `NotificationProvider`: Platform hook that renders a payload as a native system notification.
* **scheduler**: Scheduler implementation.
    * `Job`: A persistent background execution unit handled by the scheduling subsystem.
    * `JobFactory`: Translates Jobs into executable runtime actions. `NoOpJobFactory` is a no-op fallback.
    * `JobStore`: Persistence contract for the job queue with a `LocalJobStore` implementation as an in-memory fallback.
    * `Scheduler`: Orchestrates dispatch logic by managing job queues destined for persistent execution. Implemented by `JobScheduler`.
    * `JobResult`: Represents the final outcome of a `Job`.
* **translation**: Translation service.
    * `TranslationStore`: Persistence contract for translation data. `NoOpTranslationStore` is a no-op fallback.
    * `TranslationService`: Service to monitor locale and translations. Implemented by `TranslationManager`.

#### data

* **device**: Device specific code.
    * `Device`: Generates and fetches a device installation uuid.
* **http**: `Ktor` configuration and network logic.
    * **plugin**: Plugins used by the `Ktor` Http engine.
    * `HttpClientFactory`: Creates a `HttpClient` with the plugins.
    * `HttpClientEngine`: Creates the Http client engine.
    * `NoOpHttpClientEngine`: A no-op implementation of `HttpClientEngine`. Returns 204 for every request.
    * `Network`: Network status helpers.
    * `Http`: Extensions to execute type-safe requests and decode the response into a `HttpResult<Success|Error>`.
    * `HttpResult`: Sealed `Success`/`Error` result type returned by every request, so callers pattern-match instead of catching.
* **database**: SQL driver creation.
    * `SqlDriver`: Creates a SQLDelight async database driver.
    * `NoOpSqlDriver`: A no-op implementation of `SqlDriver`.
    * `SqlIO`: Query helpers.
* **storage**: File system access.
    * `File`: Declarations for suspending save/load/delete file operations.
    * `StorageFile`: A file abstraction that allows choosing if it is `encrypted` and has an optional lazily-hydrated cache.
    * `CoreFile`: Index of every file this module persists.

#### ui

* **component**: UI composite components.
    * `Store`: `ViewModel` with a `StateFlow<State>` as the single source of truth for the UI and reducer override to process actions from the UI.
* **devicelocation**: Device location composables.
    * `DeviceLocationProvider`: Provides `LocalDeviceLocationProvider` and a lifecycle register which starts/stops capture as the app foregrounds/backgrounds.
* **lifecycle**: Platform-aware lifecycle observers.
    * `Lifecycle`: Composable that registers foreground/background callbacks, with an optional recomposition key.
* **media**: Media composables.
    * `AudioPlayer`: Provides an Audio Player with its registered foreground/background lifecycle.
    * `Camera`: Provides a Camera with its registered foreground/background lifecycle, plus the `CameraPreview` composable that renders the platform preview surface.
* **modifier**: Modifier extensions.
    * `PointerListener`: Modifiers for taps or mouse clicks.
* **navigation**: Routing logic.
    * **scene**: Scene strategies used to render a list of entries, like `SplitScene` that renders two entries side-by-side when both opt in via metadata.
    * `Router`: Interface declaring the backstack and navigation functions `back()`, `navigate(screen, option)` and the `NavOption` strategy enum.
    * `Navigator`: Implementation of `Router` to handle routing logic and manage the navigation backstack.
    * `NoOpRouter`: A no-op implementation of `Router`.
    * `Routing`: Hosts the navigation display and handles back gesture/mouse-back. Provides `LocalRouter`.
* **nfc**: NFC composables.
    * `NfcController`: Provides a NFC controller with its registered foreground/background lifecycle.
* **push**: Push composables.
    * `Push`: Lifecycle registers for Push and Broadcast manager, which close the WebSocket and SSE connections on background and reopen them on foreground. See [Client Connection Lifecycle](#client-connection-lifecycle).
* **permission**: System permissions management.
    * `PermissionManager`: Manager for checking and requesting system permissions declared in `Permission`. The latter also provides `LocalPermissionManager`.
* **screen**: UI entry points built with components. Each screen is also a sub-package containing the composable and respective store for state management, all co-located.
    * `Screen`: Wrapper composable that provides the foundational UI. Provides `LocalScaffold`.

#### root level

* `App`: Holds the app related data like id, name and version of the app consuming this module.
* `AppCore`: Composable that wraps content in every provider this module owns, so a consuming app opts into the whole module with one call.
* `KInitializer`: Android-only startup initializer that captures the `applicationContext`, so no other Android code needs a context injected. See [Context Management](#context-management).

### Client App

#### data

* **database**: `SQLDelight` implementations, tables and drivers.
    * **adapter**: List of adapters used to marshal and map data types to and from a database.
    * `DatabaseFactory`: Creates the main `AppDatabase` with the adapters.
    * `Database`: Schema aliases and database extension helpers.
* **resource**: Resource index.
    * `AudioResource`: Sealed class functioning as a resource index for audio in `commonMain/resources/tracks`.
    * `ImageResource`: Sealed class functioning as a resource index for images in `commonMain/resources/drawable`.
    * `JsonResource`: Sealed class functioning as a resource index for JSONs in `commonMain/composeResources/files`.
    * `ResourceLoader`: Loads resources via the Compose Resources API.
* **storage**: File system access.
    * `Files`: The collection of all app files.
    * `AppFile`: Index of every file this module persists.

#### domain

* **push**: Push implementations. 
    * `PayloadService`: Start/stop contract for the payload listener.
    * `PayloadManager`: Implementation of `PayloadService` that collect payloads from `PushProvider` and routes them.
* **nfc**: NFC tag handling.
    * `TagService`: Start/stop contract for the tag listener.
    * `TagManager`: Implementation of `TagService` that collects scanned tags from `TagProvider` and acts on the `NfcRecord`s they carry.
* **scheduler**: Scheduler implementation.
    * `JobProvider`: Implements `JobFactory` to resolve persisted `Job`s.
    * `JobSchedulerStore`: Implements `JobStore`.
    * `Mappers`: Converts between the persisted job row and the domain `Job`.
* **gateway**: Implementation of specific business workflows. Each feature is a sub-package containing a `UseCases` interface, a `Repository` interface, and the `Gateway` implementing both.
  * `UseCases`: Aggregates every feature's use cases interface.
  * `Repositories`: Aggregates every feature's repository interface.
  * `Gateways`: Constructs every `Gateway` and implements both `UseCases` and `Repositories` by delegating to them.

#### ui

* `App`: Main composable that assembles the application UI and acts as the top-level container for the user-facing elements. Provides `LocalAppState`, `LocalAudioPlayer`, `LocalCamera` and `LocalNfcController`.
* **navigation**: Routing logic.
    * **provider**: Definitions of the navigation routes.
    * `Screen`: Sealed interface that enumerates all destinations.
    * `Navigation`: Composables that set up the navigation and define the possible navigation destinations within the app.
* **component**: UI components implementations. Each component is a sub-package containing the composable and respective store for state management, all co-located.
* **screen**: UI entry points built with components. Each screen is also a sub-package containing the composable and respective store for state management, all co-located.

#### root level

* `Dependency`: Manual dependency injection, split into an application-wide graph and a per-user graph.
    * `Dependency.AppGraph`: Initializes the app-wide stack like `SqlDriver` → `AppDatabase`, `HttpClientEngine` → `HttpClient`, `Scheduler`, push and translations services, and holds a default guest user.
    * `Dependency.UserGraph`: Per-user stack built on top of `AppGraph` to provide authenticated `Gateways`.
* `Application`: Entry point of the application. It initializes the `Dependency` graph and other app services. Provides dependency `Flow`s that can be observed to ensure initialization is complete.

#### platform entry points

Each target boots the same `App` composable through its own host.

* `MainActivity` (Android), `MainViewController` (iOS), `main` (Desktop and Web).
* `NfcActivity` (Android): Transparent no-UI activity that receives tag intents when `MainActivity` isn't foreground, hands the tag to `TagProvider`, then relaunches `MainActivity`.
* `FcmMessagingService` (Android), `FcmMessaging` (iOS), `FcmMessaging` (Web): Receive FCM messages and forward them into `PushProvider`.

### Server

#### core

* **platform**: OS-specific APIs.
    * `Env`: Environment variables definitions.
    * `Property`: Property definitions.
* **security**: Cryptography and UUID utilities.
    * `Cryptography`: Cryptography methods to hash and verify passwords.
    * `Access`: JWT access token creation/verification and refresh token generation.
* **telemetry**: Logging and crash reporting.
    * `ServerLogger`: A `TelemetryEngine` implementation for server outputs.

#### data

* **database**: `Exposed` implementations and tables.
    * **table**: Database tables definitions.
    * `DatabaseFactory`: Creates a `R2dbcDatabase`.
    * `Database`: Database extension helpers.
* **http**: `Ktor` configuration and network logic.
    * **plugin**: Plugins used by the `Ktor` Http engine.
    * `HttpServerFactory`: Sets the Http Server with the plugins.
* **signal**: Cross-instance signaling for push delivery. See [Multi-Instance Scaling](#multi-instance-scaling).
    * `InstanceSignal`: Pub/Sub interface for signaling events to every server instance observing a channel.
    * `LocalSignal`: Single instance implementation.
    * `PostgresSignal`: Multi-instance implementation backed by Postgres `LISTEN`/`NOTIFY`.

#### domain

* **permission**: Permission service.
    * `PermissionService`: Tracks each user's permissions. Implemented by `PermissionManager`. Every authenticated route validates against this map instead of a fixed role.
    * `AccessToken`: The decoded JWT claims a route reads to identify the caller.
* **push**: Broadcast and Push services.
    * `BroadcastService`: Broadcast service to send push notifications. Implemented by `BroadcastManager`.
    * `PushService`: Push service to send user push notifications, including acknowledgement of delivered notifications. Implemented by `PushManager`.
    * `FcmService`: Sends push payloads via Firebase Cloud Messaging. Implemented by `FcmManager`, with `NoOpFcmService` used in development so no Firebase project is needed.
    * `Delivery`: Shared functions for sending payloads across the WebSocket/FCM channels.
* **route**: Defines the routing endpoints for the server.
    * **provider**: Aggregates the routes.
    * `Routing`: Sets all routes defined in the `provider` package.
    * `Extensions`: Route helpers, including permission validation and the request-audit logging every route calls.
* **usecase**: Implementation of specific business workflows. Each use case is a sub-package containing the interface and its gateway implementation.
    * `Gateways`: Aggregates all gateways under the `UseCases` file.

#### test

`TestCase` and other fixtures used by the test source set.

#### root level

* `Dependency`: Manual dependency injection. Receives an already-built `R2dbcDatabase` and wires `UseCases` via `Gateways`.
* `Application`: Entry point of the server application. It builds the `R2dbcDatabase`, initializes the `Dependency` graph and other app services, and seeds one super-user account on every boot.

# Development Workflow

## Environment Setup

By default, signing keys and some envs are read from a `local.properties` file in the root directory.
Change this method at your leisure.
The file is optional: it is loaded only if it exists and every key falls back to an empty default, so a missing one costs you signed release builds and Sentry reporting, not a working development environment.

* **Android Signing**: `android.storeFile`, `android.keyAlias`, `android.keyPassword`, `android.storePassword`.
* **Mac Notarization**: `mac.sign.identity`, `mac.notarization.appleId`, `mac.notarization.teamId`, `mac.notarization.password`.
* **Sentry Monitoring**: `sentryDsn` for production monitoring.
* **Web Push**: `web.vapidKey`, `web.firebaseConfig`.

The server instead reads its configuration from OS environment variables via `Env.kt`:

* **Server**: `DEVELOPMENT`, `PORT`, `SENTRY_DSN`.
* **Admin**: `ADMIN_UUID`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`
* **Database**: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.
* **JWT**: `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_SECRET`.
* **Push**: `GCP_PROJECT_ID`.

Every one of these is nullable, and nothing supplies a default in code. What makes `./gradlew :server:run` work with none of them set is `server/build.gradle.kts`, which defines a `localEnv` map and injects it into both the `run` task and every `Test` task:

```
DEVELOPMENT=true
ADMIN_UUID=FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin
JWT_ISSUER=jwt_issuer
JWT_AUDIENCE=jwt_audience
JWT_SECRET=jwt_secret
```

Server tests get the same environment as `run`, which is why they need no setup of their own. 
The dev environment is a property of the **Gradle task**, not of the code, so launching the built fat jar directly gives you none of it: it will start in production mode and expect a real Postgres, a real JWT secret and real `ADMIN_*` values.

## Development vs Production

| Concern         | Description                                                            | Dev                                   | Production                           |
|-----------------|------------------------------------------------------------------------|---------------------------------------|--------------------------------------|
| Database        | Where persistent data is stored                                        | In-memory H2, resets on restart       | Postgres                             |
| Signal          | Cross-instance pub/sub used to wake up sockets held by other instances | `LocalSignal` (in-process)            | `PostgresSignal` (`LISTEN`/`NOTIFY`) |
| FCM             | Push delivery when a device has no live WebSocket/SSE connection       | `NoOpFcmService`                      | `FcmManager` (*1)                    |
| JWT             | How access tokens are signed and verified                              | HMAC512 with the injected secret      | HMAC512 with the real `JWT_SECRET`   |
| Telemetry       | Where logs are sent                                                    | Console `ServerLogger`                | Sentry, if `SENTRY_DSN` is set       |
| CORS            | Which origins the server accepts requests from                         | `localhost:8080` and `localhost:8010` | HTTPS-only on `URL.HOST`             |
| Time validation | Whether mutation requests with a stale/future timestamp are rejected   | Skipped                               | Enforced                             |
| Seeded account  | The one admin account created on every boot                            | `admin`/`admin`                       | Real `ADMIN_*` values                |

The switch is `Env.developmentMode`, set from the `DEVELOPMENT` environment variable, which the `:server:run` and test tasks inject as `true`.

(*1) FCM needs three things to activate: production mode, a non-blank `GCP_PROJECT_ID`, and the `firebase` `ServerFlags` flag enabled. 
If any is missing, the server falls back to `NoOpFcmService` rather than failing to boot. so a misconfigured deployment silently delivers over WebSocket and SSE only.
The signal is chosen by the connection factory rather than by the flag: a Postgres factory yields `PostgresSignal`, anything else yields `LocalSignal`.

## API Routes

Authorization is per `EntityType`. Each user carries a permission map, checked by `validatePermission` on every protected route. 
Most route groups are also gated by a `ServerFlags` flag. `Config` and the request-audit `Registry` routes are installed unconditionally. 
Paths come from the shared `URL` registry.

| Method & Path                                                | Purpose                                                        | Auth                                              | Flag             |
|--------------------------------------------------------------|----------------------------------------------------------------|---------------------------------------------------|------------------|
| `GET /`                                                      | Liveness text                                                  | —                                                 | —                |
| `HEAD /api/probe`                                            | Health check                                                   | — (rate-limited)                                  | `probe`          |
| `GET /api/clientflags` · `/api/clientconfigs`                | Serve current client flags/configs                             | —                                                 | —                |
| `POST /api/clientflags` · `/api/clientconfigs`               | Update client flags/configs                                    | JWT, `CLIENT_FLAG`/`CLIENT_CONFIG` `WRITE`        | —                |
| `GET`/`POST /api/serverflags` · `/api/serverconfigs`         | Read/update server flags/configs                               | JWT, `SERVER_FLAG`/`SERVER_CONFIG` `READ`/`WRITE` | —                |
| `GET /api/registry`                                          | Delta-sync the request audit log recorded by every route below | JWT, `REGISTRY` `READ`                            | —                |
| `POST /api/registry`                                         | Record a registry entry                                        | JWT, `REGISTRY` `WRITE`                           | —                |
| `GET /api/data/*`                                            | Static resources                                               | —                                                 | `resources`      |
| `POST /api/register`                                         | Create an account                                              | JWT, `SESSION` `WRITE`                            | `authentication` |
| `POST /api/login`                                            | Login a user                                                   | —                                                 | `authentication` |
| `POST /api/logout` · `/api/refresh-tokens`                   | Logout / rotate tokens                                         | refresh token                                     | `authentication` |
| `GET (SSE) /api/broadcast` · `POST /api/broadcast/subscribe` | Broadcast stream / FCM topic subscribe                         | — (rate-limited)                                  | `broadcast`      |
| `POST /api/broadcast`                                        | Send a broadcast                                               | JWT, `NOTIFICATION` `WRITE`                       | `broadcast`      |
| `POST /api/device-tokens` · `/api/tickets`                   | Register FCM token / mint a WebSocket ticket                   | JWT                                               | `push`           |
| `WS /api/push`                                               | Push connection; inbound frames are delivery acknowledgements  | ticket                                            | `push`           |
| `POST /api/push`                                             | Send a targeted push                                           | JWT, `NOTIFICATION` `WRITE`                       | `push`           |
| `GET /api/users`                                             | List users                                                     | JWT, `USER` `READ`                                | `users`          |
| `GET /api/users/{uuid}`                                      | Fetch a single user by uuid                                    | JWT, `USER` `READ`                                | `users`          |
| `POST /api/users`                                            | Upsert a user (*1)                                             | JWT, `USER` `WRITE`                               | `users`          |
| `POST /api/device-location`                                  | Report a captured device location fix                          | JWT, `DEVICE_LOCATION` `WRITE`                    | `deviceLocation` |
| `GET /api/tasks`                                             | Delta-sync tasks                                               | JWT, `TASK` `READ`                                | `tasks`          |
| `GET /api/tasks/{uuid}`                                      | Fetch a single task by uuid                                    | JWT, `TASK` `READ`                                | `tasks`          |
| `POST /api/tasks`                                            | Upsert a task                                                  | JWT, `TASK` `WRITE`                               | `tasks`          |
| `GET /api/[feature]`                                         | Delta-sync feature                                             | JWT, `ENTITY` `READ`                              | feature-specific |
| `GET /api/[feature]/{uuid}`                                  | Fetch a single entity by uuid                                  | JWT, `ENTITY` `READ`                              | feature-specific |
| `POST /api/[feature]`                                        | Upsert a feature                                               | JWT, `ENTITY` `WRITE`                             | feature-specific |

(*1) A user may always update itself, only `SESSION` `WRITE` may modify another user or change permissions.

## Push Notifications

Delivery uses two independent channels per push type, so devices are covered whether they are currently reachable over a live connection or not:

* **Broadcast** (`BroadcastService`) sends to both the SSE stream and the FCM broadcast topic, to every listener, regardless of which devices are connected to which.
* **Push** (`PushService`) sends to a user's live WebSocket connections or via FCM. For a direct, non-persisted send (`POST /api/push`) there is no confirmed-delivery handoff between the two channels: if a WebSocket send fails after the FCM exclusion list was already computed, that specific notification can be missed until the next one triggers a fresh, now-accurate presence check.

Entity-triggered notifications take a more durable path instead: `NotificationsGateway.saveNotification` persists the payload to `NotificationTable` before delivery is attempted, `PushManager` observes that table per user (cross-instance, via `InstanceSignal`) and redelivers on every change, and a row is only deleted once the client acknowledges it. 
A device that reconnects after being offline gets replayed its full backlog of unacknowledged notifications, giving this path at-least-once delivery instead of the best-effort behavior above.

Any payload that does happen to arrive over both channels is deduplicated client-side by `PushProvider`, which remembers recently handled payload UUIDs.

### Multi-Instance Scaling

Both services are built to run correctly behind multiple server instances, with no state that only one instance can see:

* **Connection tickets** (`TicketTable` / `TicketUseCases`): the short-lived, single-use ticket a client exchanges for a WebSocket connection is persisted rather than held in memory, so a ticket minted by one instance is redeemable on any other. Consumption is atomic, so a ticket can never be redeemed twice even by concurrent requests hitting different instances.
* **Connection presence** (`ConnectionTable` / `ConnectionUseCases`): which devices are connected, and to which instance, lives in the database instead of a local map. Each instance heartbeats its own rows and sweeps rows left behind by crashed instances, so the FCM-exclusion decision above is correct globally, not just for sockets held by the instance handling sending pushes.
* **Cross-instance signaling** (`InstanceSignal`): a notification saved, or a broadcast sent, on one instance must still reach a WebSocket/SSE connection held by a different instance. `PostgresSignal` uses Postgres `LISTEN`/`NOTIFY`, backing every channel with a single shared, self-healing connection regardless of how many observers subscribe to it. The signal only carries deliveries to *other* instances: an instance sends to its own SSE sessions directly and tags each relayed payload with its instance id, so it skips its own echo and a signal failure can never black out the sessions it holds itself.

### Client Connection Lifecycle

`RegisterBroadcastLifecycle` and `RegisterPushLifecycle` decide whether a platform keeps its WebSocket/SSE connections open while the app is backgrounded, or relies on FCM alone:

* **Android / iOS**: connections close on background and reopen on foreground. FCM is the only channel while backgrounded. It's a store-and-forward mechanism built to wake up a backgrounded or killed app, not for high-frequency low-latency delivery, and the OS suspends sockets shortly after backgrounding anyway.
* **Web**: connections stay open while the tab is open, alongside an FCM service worker for background delivery.
* **Desktop**: there is no FCM client SDK for JVM desktop apps, so connections always stay open for the app's lifetime and FCM is never used on this platform.

### Firebase Setup

None of this is required to develop locally: the server runs with `NoOpFcmService` in development, and the WebSocket and SSE channels work without any Firebase project. Follow these steps only when you want FCM delivery working end-to-end.

All commands assume the [Firebase CLI](https://firebase.google.com/docs/cli) is installed and logged in (`firebase login`).

1. Create or reuse a Firebase project and register an app per platform, e.g. `firebase apps:create ANDROID <appId>`, `firebase apps:create IOS <appId>`, `firebase apps:create WEB "<appName>"`.
2. **Android**: `firebase apps:sdkconfig ANDROID <appId>` and save the result as `clientApp/google-services.json`.
3. **iOS**: `firebase apps:sdkconfig IOS <appId>` and save the result as `iosApp/iosApp/GoogleService-Info.plist`.
   * In Xcode, open `iosApp.xcworkspace` → select the `iosApp` target → **Signing & Capabilities** → pick a Team → **+ Capability** → add **Push Notifications**. This registers the capability on the App ID and wires the existing `iosApp.entitlements` file via the `CODE_SIGN_ENTITLEMENTS` build setting.
   * `Info.plist` already declares `UIBackgroundModes: remote-notification`, needed for silent payloads to reach the app while backgrounded. Visible payloads are rendered by iOS itself from the APNs `alert` field and don't need this.
4. **Web**: `firebase apps:sdkconfig WEB <appId>` and set the result, minified to a single line, as `web.firebaseConfig` in `local.properties` (e.g. `web.firebaseConfig={"apiKey":"...","projectId":"...",...}`).
   * Generate a VAPID key: Firebase Console → Project Settings → Cloud Messaging → Web configuration → "Generate key pair" (no CLI/API path exists for this). Set it as `web.vapidKey` in `local.properties`.

## Feature Creation

New features should be gated behind a flag. Below are the steps to create a new isolated feature.

#### Shared

1. **Domain**: Add the domain file(s) in the `domain` package.
2. **Entity**: Add the entity to `EntityType` for offline support.
3. **Flag**: Add the client and server flags for the feature: `val {feature}: Boolean`.

#### Design

1. **UI Components**: Add the needed components.

#### Client App

1. **Components with Store**: Create a new package `ui/component/{component}` with files `{Component}`, `{Component}StateAction`, `{Component}Store`.
2. **Screen**: Create a new package `ui/screen/{feature}` with files `{Feature}Screen`, `{Feature}ScreenStateAction`, `{Feature}ScreenStore`.
3. **Gateway**: Create a new package `domain/gateway/{feature}` with the `{Feature}Gateway`, a `{Feature}UseCases` and/or `{Feature}Repository` interface for it to implement, and a `Mappers` file converting between the domain model and the generated SQLDelight schema, plus the needed `.sq` files in the folder `sqldelight`.
4. **Scheduler**: Handle the new entity in `domain/scheduler/JobProvider`, which switches on `EntityType` and `Job.Type` and delegates to the feature's `Repository`.
5. **Navigation**: Add to the `ui/navigation` package the new screen in the sealed class `Screen`, the new route in the `provider` package and the link it in the composable navigation in `Navigation`, gated by the feature flag.

#### Server

1. **Database**: Add the feature table to `data/database/table` and to the table list in `Database`.
2. **UseCase**: Create the use cases in `domain/usecase`.
3. **Route**: Add the feature routes in `domain/route` in the `provider` package and the link it in the `Routing` file, gated by the feature flag. Validate access and log request telemetry.

#### UseCases VS Repositories

UseCases and Repository aren't "domain logic" vs "data access". They are two different consumer contracts on the same implementation: UI-facing API vs. cross-gateway API. 
Splitting them means a `Store` only sees the narrow surface it needs, and a sibling gateway that needs to read data, without triggering scheduler logic, depends on a Repository, not the full UseCases.
So it is about giving the narrowest interface for their purpose: screens get the UI/write API (`UseCases`), other gateways/scheduler get the read/sync API (`Repository`), while keeping one concrete Gateway per feature so there's no duplicated implementation or state to keep in sync between two classes.

## Registries

Every kind of resource, endpoint and screen has one registry that owns it, and the codebase reaches for the registry entry instead of the raw string.
The objective is to scale more easily, so that adding something is a compile-time change in one known place, and removing something breaks the build rather than failing silently at runtime.

### Assets

| Type        | Drop the file in                                     | Register it in                | Read it with                             |
|-------------|------------------------------------------------------|-------------------------------|------------------------------------------|
| Audio track | `clientApp/src/commonMain/resources/tracks`          | `data/resource/AudioResource` | `AudioPlayer`                            |
| Image       | `clientApp/src/commonMain/composeResources/drawable` | `data/resource/ImageResource` | `ImageResource.X.drawable`               |
| JSON file   | `clientApp/src/commonMain/composeResources/files`    | `data/resource/JsonResource`  | `loadResource<T>(json = JsonResource.X)` |

Each registry is a `sealed class` whose entries carry the relative path, so a renamed or deleted asset surfaces in exactly one file.
`ImageResource` additionally pairs the path with the generated `Res.drawable` reference, so the UI never touches generated identifiers directly.

### Persisted Files

Files persisted on the device are declared as `StorageFile` objects, one per payload, and never opened by path.

1. Add the object to the module's index: `CoreFile` for anything `appCore` owns, `AppFile` for anything the `clientApp` owns.
2. Add it to that index's `all` list.
3. Access it directly on the `StorageFile` object.

`Files.kt` concatenates every module index into `files`, which is what a full wipe iterates over, and `logFiles()` prints the registry at startup with a warning when two files claim the same path.

### Domain & Network

| Adding            | Register it in                                                                                     |
|-------------------|----------------------------------------------------------------------------------------------------|
| Entity Type       | `shared/domain/EntityType` - required by the offline engine                                        |
| Endpoint          | `shared/data/http/URL`                                                                             |
| Http Header       | `shared/data/http/Header`                                                                          |
| Query Parameter   | `shared/data/http/Query`                                                                           |
| Push Payload Type | `shared/domain/PushPayload` - then handle it in `clientApp/domain/push/PayloadManager              |
| Feature Toggle    | `ClientFlags` and `ServerFlags`                                                                    |
| Tunable Value     | `ClientConfigs` and `ServerConfigs`                                                                |
| Translation       | `main/resources/static/translations.json` - mirrored in `composeResources/files/translations.json` |

Flags and configs are shared types: the server serves them, the client persists and hot-reloads them, and both sides read the same field.

### Client

| Adding           | Register it in                                                                                                      |
|------------------|---------------------------------------------------------------------------------------------------------------------|
| Screen           | `ui/navigation/Screen` with a stable `@SerialName`, an entry in `ui/navigation/provider` and the `Navigation` graph |
| Background job   | `domain/scheduler/JobProvider`                                                                                      |
| Permission       | `ui/permission/Permission`, then implement it in each platform `PermissionManager` implementation                   |
| Telemetry Engine | `Telemetry.engines`                                                                                                 |

### Server

| Adding   | Register it in                                           |
|----------|----------------------------------------------------------|
| Table    | `data/database/table`, plus the table list in `Database` |
| Route    | `domain/route/provider`, plus the link in `Routing`      |

### Tests

Fake payloads belong in `sharedTest`'s `FakeData` and mock responses in each module's mock engine, so a new endpoint is answered once and every module's tests get it for free.
A new entry in any registry above generally means a new branch in `createMockHttpEngine` and a new `FakeData` value, not a new mock per test.

## UI Specifics

Compose screens use a custom **MVI Store pattern**. Each screen has a Store (`{Screen}Store`) and a Screen composable (`{Screen}Screen`). Jobs inside a Store can be launched with an ID to cancel/replace prior work.
The UI state uses `kotlinx.collections.immutable` to prevent accidental mutations and to keep state classes reliably stable for Compose.

`Store<State, Action>` (extends `ViewModel`) is the base class:

* `stateFlow: StateFlow<State>` — single source of truth observed by the composable
* `state` — a non-reactive snapshot of the current state, for reads inside a reducer
* `send(action)` → `reducer(state, action)` — the only entry point for UI events
* `updateState { }` — the only way to mutate state, applied atomically
* `launch(id, replace, context) { }` — launches a coroutine tied to an ID. `replace = true` (the default) cancels the previous run of that ID; `replace = false` keeps the in-flight one and drops the new request
* `Flow<T>.observe(id) { }` — lifecycle-aware collection driven by the store's own subscription count, pausing after a grace period once the UI unbinds and resuming when it reattaches
* `Flow<T>.toStateFlow(started, initialValue)` — shares a cold flow into the `viewModelScope` as state

### Navigation

`Router.navigate(screen, option)` takes a `NavOption` strategy for mutating the backstack (matching is by class, not equality):

* `ADD` — pushes the screen onto the top
* `CLEAR` — wipes the backstack, then pushes
* `REPLACE_FIRST` / `REPLACE_LAST` — removes the first/last instance of that screen class and everything above it, then pushes
* `SWAP` — moves the last instance of that screen class to the top, preserving its state, instead of pushing a new one
* `POP` — pops back to the last instance of that screen class, discarding the new instance passed in
* `IGNORE` — pushes only if that screen class isn't already on top

## Translations

Translations are defined in the server file `main/resources/static/translations.json`.
This file is fetched by the client `TranslationService`, which also caches the translations in its `TranslationCache`.
Translations are also bundled with the client in a mirror file `commonMain/composeResources/files/translations.json` to protect the client translations from network unavailability.
Adding new translations is as simple as adding new entries to the server file and optionally its mirror to the client file (recommended).
The `Translations` file offers methods to collect the translation cache state, get and inject translations.
Translations should be fetched through this file and not by calling `TranslationCache` directly, as the former resolves translations against the cache state, so the composable recomposes whenever the cache changes, e.g. the user switches locale.
A direct cache call is a plain read with no state behind it.

### JSON Structure

The `translations.json` file represents a standard localization (i18n) dictionary. It maps string keys to their corresponding human-readable text for a specific language.
Each object in the array defines a single translated string and consists of three properties:

* languageIso: The ISO 639-1 two-letter language code identifying the language of the string (e.g., "en", "pt").
* key: The unique identifier used within the codebase to request this specific piece of text (e.g., hello_world).
* value: The actual localized text that will be displayed to the user on the UI (e.g., "Hello World").

So a multi-language translation file would look like:

```
[
  {
    "languageIso": "en",
    "key": "hello_world",
    "value": "Hello World"
  },
  {
    "languageIso": "pt",
    "key": "hello_world",
    "value": "Olá Mundo"
  },
  {
    "languageIso": "es",
    "key": "hello_world",
    "value": "Hola Mundo"
  }
]
```

### Usage

In the composables, call `getTranslation(key, args)` to get the localized string.
The language ISO is handled automatically, only the `key` is needed; `args` is used to populate positional placeholders defined in the translation. Example:

```
{
  "languageIso": "en",
  "key": "translation_with_arguments",
  "value": "I have 2 arguments %1$s and %2$s"
}
```

Calling:

```
getTranslation("translation_with_arguments", "one", "two")
```

Returns:

`I have 2 arguments one and two`.

To inject translations in the cache and bypass the `TranslationService`, call `InjectTranslations(translations)`.
This is specially useful for composable previews.

## Context Management

On Android, use the `applicationContext` provided via `KInitializer` to avoid explicit context injection.

## Testing

The testing structure mirrors the source code to ensure 1:1 coverage.
Code coverage is measured using `Kover`, configured independently per module but with the same exclusion rules everywhere: generated code, `@Serializable` classes, `@Preview` composables and anything annotated with `@ExcludeFromTesting`.
This last annotation is preferred instead of explicit, less scalable declarations in `Gradle`.
`Kover` is configured to verify a minimum of **90% code coverage**.

Test classes, fake data and mocks live in the [Shared Test](#shared-test) module and are reused by every other module.
Most modules implement the test classes with its specific `TestCase` which provides the harness for hermetic testing and provides:

* An in-memory database and a mock Http engine, so no test touches a real socket or a real file.
* `runUnitTest { }` — A function that sets up the test environment, resets all data, runs unit test then tears down.
* `runUITest { }` — Same process as the unit test but for UI test. The `setUI { }` is used to render composables under `AppTheme` with mock lifecycle and navigation.
* `runServerTest { }` — Same process as the unit test but with an environment that allows testing Http routes end-to-end.

All three point Dispatchers `Main`, `Default` and `IO` at one `UnconfinedTestDispatcher`, reset every client and server flag and config to its defaults, and swap the telemetry engines for a `MockLogger`, so no test inherits state from the one before it.
Server tests additionally receive the same injected environment as `./gradlew :server:run` (see [Environment Setup](#environment-setup)), which is why they boot against in-memory H2 with a seeded admin and need no configuration of their own.

Tests should be written in the common test directory, like `commonTest`, whenever possible.

Run tests using:

* **Shared**: Run all tests using `./gradlew clean testSharedAndReport`.
* **App Core**: Run all tests using `./gradlew clean testAppCoreAndReport`.
* **Client App**: Run all tests using `./gradlew clean testClientAndReport`.
* **Server**: Run all tests using `./gradlew clean testServerAndReport`.

## Platform Specific Implementations

These are the declarations that resolve to a distinct implementation per source set.

* **SqlDriver**: Creates a SQLDelight async database driver.
* **HttpClientEngine**: Creates a Ktor HTTP engine.
* **Network**: Checks internet connectivity and exposes a `Flow<Boolean>` for real-time state changes.
* **File**: IO operations on the local filesystem.
* **AppPath**: Resolves the absolute paths to the application's data and cache directories.
* **Development**: A boolean flag indicating debug mode.
* **Cryptography**: Encrypt, decrypt, and hash string content.
* **PlatformLogger**: A `TelemetryEngine` that writes to the native system console.
* **AudioPlayer**: Creates a platform audio player for playlist control.
* **Camera**: Creates a platform camera controller.
* **CameraPreview**: Composable rendering the platform camera preview surface.
* **NfcController**: Creates a platform NFC reader/writer.
* **DeviceLocationProvider**: Creates a platform device location capture controller.
* **Locale**: Reads the system language, formats UTC dates for the local timezone, and emits a `Flow` on locale changes.
* **Clock**: Emits when the OS reports the system clock changed, which is one of the signals feeding clock trust.
* **NotificationProvider**: Renders a push payload as a native system notification.
* **Lifecycle**: Composable that registers foreground/background callbacks.
* **PermissionManager**: Creates the platform permission manager.
* **Platform**: Runtime metadata (OS name, version, brand, model).
* **Dispatcher**: Provides the I/O `CoroutineDispatcher`.
* **VerticalScrollBar**: Renders a styled scrollbar alongside a `LazyList`.
* **PlatformTestCase**: Abstract base class for platform-specific test setup.

## Deployment & Distribution

Bump versions with:

```shell
./gradlew bumpVersion [-PnewVersion=x.y.z] [-PnewCode=x] [-PnewServerVersion=x.y.z]
```

This updates every place the version lives:

- `build-logic/convention/src/main/kotlin/Shared.kt`
    - appVersion, appVersionNumber, appServerVersion
- `iosApp/iosApp.xcodeproj/project.pbxproj`
    - MARKETING_VERSION, CURRENT_PROJECT_VERSION

Standard compile commands for different platforms include:

* **iOS**: Xcode archive
* **Android**: `./gradlew clean :clientApp:bundleRelease`
* **Mac**: `./gradlew clean :clientApp:notarizeDmg --no-configuration-cache`
* **Windows**: `./gradlew clean :clientApp:packageMsi`
* **Linux**: `./gradlew clean :clientApp:packageDeb`
* **Web**: `./gradlew clean :clientApp:wasmJsBrowserDistribution`
* **Server**: `./gradlew :server:buildFatJar`

Module-qualify the tasks (e.g. `:clientApp:`, `:designShowcase:`) since multiple modules share the same unqualified task names.

# Offline Engine

The offline engine is the mechanism that makes the app Offline First.
It provides a persistent, fault-tolerant job queue backed by the database that continues executing background work when connectivity is interrupted, and resumes automatically when the device comes back online.

## Job

A `Job` is the atomic unit of work. Every background operation is modeled as a `Job` before it touches the network.

**Key properties:**

* `uuid: Uuid` — the job's own identity and primary key, which every state update targets.
* `userUuid: Uuid` — the user that originated the request. Part of the chain key, so two accounts never serialize against each other.
* `utc: Instant` — when the job was created. This is the ordering key: within a chain the oldest job runs first.
* `entityType: EntityType` — the domain entity the job operates on.
* `entityUuid: Uuid?` — the specific entity being mutated; `null` for global collection fetches.
* `type: Type` — the operation intent: `GET`, `POST`, or `DELETE`.
* `conflictPolicy: ConflictPolicy` — how to handle duplicate jobs; defaults to `IGNORE` for `GET` and `APPEND` for `POST`/`DELETE`.
* `payload: String?` — a JSON string carrying the execution parameters.
* `state: State` — the current lifecycle position: `PENDING` → `RUNNING` → `COMPLETE` / `FAILED` / `CANCELED`.
* `attempt: Int` — a zero-indexed retry counter incremented on each failure.

`toString()` is overridden to omit `payload`, so a job logged on a failure path cannot leak the contents it was carrying.

## Conflict Policies

When a job is queued, the scheduler checks the `ConflictPolicy` before writing to disk:

* **`APPEND`** — enqueue the new job as-is, alongside any pending duplicates (default for `POST` and `DELETE`)
* **`REPLACE`** — cancel all pending duplicates, moving them to `CANCELED`, then enqueue the new job
* **`IGNORE`** — skip the new job entirely if a duplicate is already pending (default for `GET`)

What counts as a "duplicate" is worth stating precisely, because it is not a full equality check.
Two jobs collide when they share `userUuid`, `entityUuid`, `entityType` and `type`, and the existing one is still `PENDING`. **`payload` is deliberately not part of the comparison.**

That is what makes the defaults correct rather than arbitrary. A `GET` is a request to be up to date, so queueing three collection syncs for the same entity type is pointless: `IGNORE` collapses them into the one already waiting, and it will pull whatever is newest when it runs. 
A `POST` or `DELETE` carries a distinct user intent in its payload, so collapsing two would silently drop an edit; `APPEND` keeps both and the chain replays them in order.
`REPLACE` is the deliberate exception for last-write-wins work, where only the final state matters and the intermediate ones are noise.

The corollary of excluding `payload`: if two `GET`s for the same entity type genuinely need different parameters, `IGNORE` will discard the second. Use `APPEND` for those.

## Chain System

Jobs are grouped into logical **chains** identified by `(userUuid, entityType, entityUuid)`.
Within a chain, only the oldest pending job runs at a time, ensuring sequential mutation ordering for the same entity without blocking unrelated entities.
The set of chains currently executing is held in memory as `activeChains`, guarded by its own mutex, because the dispatch loop is reactive: a new database emission can arrive while previous jobs are still running, and without that registry the loop would dispatch the same chain twice.

There are two job categories with distinct execution rules:

* **Targeted jobs** (`POST`, `DELETE`, `entityUuid != null`) — Mutations on a specific entity. Jobs for Entity A execute sequentially while jobs for Entity B run in parallel on a separate chain.
* **Global jobs** (`GET`, `entityUuid == null`) — Full collection syncs, which read and write across the whole table for that entity type.

The two categories are **mutually exclusive per entity type**, and the exclusion is enforced in both directions:

* A global job will not start while any targeted job for that entity type is pending *or* actively running. Local mutations are flushed to the server before the app pulls remote state, so a sync cannot overwrite an optimistic write that hasn't been pushed yet.
* A targeted job will not start while a global job for that entity type is active. A full sync is midway through reading and rewriting that table, so admitting a mutation into it would interleave a write with a bulk read that has already been partially applied.

Together these mean that, per `(user, entityType)`, the engine is doing either exactly one full sync or any number of per-entity mutation chains, never both. Different entity types are unaffected and proceed in parallel throughout.

## Job Lifecycle End-to-End

What happens when a user edits a domain while offline:

1. **Optimistic local write** — the domain is saved to SQLite in one transaction; the UI updates instantly since screens only render database flows.
2. **Enqueue** — a `Job(entityType = DOMAIN, type = POST, entityUuid = domain.uuid)` is queued; the scheduler applies the conflict policy (`APPEND` by default for `POST`) and persists it as `PENDING`.
3. **Gating** — the dispatch loop combines the pending jobs database stream, network availability and clock trust. While offline or untrusted it substitutes an empty list, so nothing dispatches and no work is lost.
4. **Dispatch** — once online and trusted, the oldest job in its chain is claimed, marked `RUNNING`, and resolved by `JobProvider` into the matching `Repository` call for that `EntityType` and `Type`.
5. **Execution** — the repository `POST`s the domain; on success the server's copy is merged back only if `remote.modifiedAt >= local.modifiedAt`, and the job transitions to `COMPLETE`.
6. **Release** — the chain is removed from `activeChains` and the terminal state is written, both inside a `NonCancellable` block so a scheduler shutdown mid-flight still records the outcome and frees the chain rather than stranding it.
7. **Read-back** — the domain list observes the database directly, so it recomposes once the write lands; a background `GET` sync for that entity type waits until this and every other pending targeted job has flushed.

## Network Awareness

The scheduler combines the pending jobs database stream with a `Network` stream and `ClockService.trusted` to gate execution.
When either gate closes, the combined stream substitutes an empty list rather than stopping, so the loop stays subscribed and simply has nothing to dispatch. 
When the device goes offline, or its clock drifts from the server, all dispatching pauses; it resumes automatically once connectivity and clock trust are restored, because the next emission from any of the three sources re-evaluates the gate.
The pending jobs stream is additionally wrapped so that a database read failure emits an empty list instead of terminating the flow. 
A transient storage error therefore costs one idle emission rather than killing the scheduler for the rest of the session.

To run the client in a fully disconnected mode, swap out `HttpClientEngine` with `NoOpHttpClientEngine` (which returns `204` for every request)
or set the feature flag `http = false` to make all outbound requests throw immediately without touching the network.

## Disabling the Scheduler

The `scheduler` feature flag does something more interesting than switching the engine off, and it is worth knowing before flipping it.
When the flag is disabled, `start()` returns immediately and no dispatch loop runs. `queue(job)` then stops being a queue operation at all: instead of persisting the job, it resolves and invokes it right away on a background coroutine.
The practical effect is that the app keeps working and every operation still executes, but it executes **eagerly, once, and without any of the guarantees the engine exists to provide**: no persistence across restarts, no retries, no chain ordering, and no connectivity or clock gating. An operation issued while offline simply fails and is gone.
That makes the flag a useful escape hatch for debugging a misbehaving queue or for a build that genuinely has no offline requirement.

## Clock Trust

On every network communication it is estimated the offset between the device clock and the server clock using an NTP-style round-trip calculation.
If the measured offset exceeds `clockDriftTolerance`, the scheduler pauses all dispatch, since a device with an unsynchronized clock cannot be allowed to arbitrate `modifiedAt` based conflict resolution against other devices.
Trust is restored automatically once a subsequent request reports an offset back within tolerance.
The offline engine is still a trust based system, as this does not fully protect against malicious users trying to actively hack the engine.

## Retry & Failure Handling

`JobResult` expresses the outcome of each execution:

* **`Success`** / **`NoOp`** → job transitions to `COMPLETE`
* **`Retry`** → attempt counter incremented; job reverts to `PENDING` if the new count is below `schedulerMaxAttempts`, otherwise transitions to `FAILED`
* **`Error`** → terminal failure, job transitions to `FAILED`

The split between retryable and terminal is made by one helper, `Throwable.toJobResult()`, which maps `InternetDisabledException` to `Retry` and everything else to `Error`.
The reasoning is that connectivity loss is the one failure the engine knows is transient by construction: the job was refused before reaching the network, nothing happened server-side, and the same job will succeed unchanged once the device is back online. 
A validation rejection, a missing permission or a serialization failure will fail identically on every attempt, so retrying them only burns the attempt budget and delays the queue behind them.
Retries carry no backoff of their own, because they don't need one. A failed job returns to `PENDING`, and the dispatch loop is already gated on connectivity and clock trust, so it will not be picked up again until the condition that is most likely to have caused the failure has cleared.

Two safety properties are worth calling out:

* **Failure is the default, not the success case.** Before running, the job is staged as `FAILED`, and only an explicit result overwrites that. An exception severe enough to escape the result mapping therefore lands on `FAILED` rather than leaving the job `RUNNING`, which is what would otherwise turn one bad job into a queue that never drains.
* **Terminal writes are non-cancellable.** Releasing the chain and persisting the final state happen in a `finally` wrapped in `NonCancellable`, so cancelling the scheduler mid-execution cannot leave a chain permanently claimed or a job permanently `RUNNING`.

## Zombie Recovery

`RUNNING` is a claim held in the database, but the coroutine holding it lives only in memory. If the OS kills the app mid-execution, the row survives and the worker does not, leaving a job that no longer runs and that the chain system will never dispatch again because it looks claimed.
The recovery is deliberately blunt: on every startup, before the dispatch loop subscribes to anything, every `RUNNING` row is reset to `PENDING` in a single statement. This is safe to do unconditionally precisely because it runs before the loop starts, so there is no live worker whose claim could be revoked by mistake.
The cost is that a job killed after its request reached the server will run a second time, which is why the write path is built to tolerate it: upserts are keyed by entity uuid and guarded by the `modifiedAt` comparison, so replaying one is idempotent rather than duplicating data.

## Delta Sync

Keeping a device's local database current with the server happens through two complementary mechanisms: 
* A bulk **collection sync** that periodically catches a device up on everything it missed.
* A **push-triggered entity sync** that reacts to a single change as soon as it's notified about it.

### Collection Sync

A collection sync is resolved to a paginated fetch. Two independent bounds shape the query:

* A fixed watermark for the entire sync: "everything before this point is already synced." It does not change while a sync is in progress.
* A walking, descending keyset cursor used purely to page through the results of *this* sync. It starts unset (page 1 returns the newest matching rows) and advances to the last item of each page.

The server applies both, ordering by `modifiedAt DESC, uuid DESC`. The uuid is a tiebreaker, not decoration: rows written in the same instant would otherwise have no stable order, and a keyset cursor over an ambiguous ordering can skip or repeat rows at a page boundary. 
The cursor is therefore the pair `(cursor_utc, cursor_uuid)`, taken from the last item of each page.
Newest-first is a deliberate choice: on a large backlog like a first sync, or a device that was offline a while, the freshest, most relevant changes land in the very first page instead of being the last thing to arrive.
The requested page size is clamped server-side, so a client cannot ask for an unbounded page.

Paging continues while a full page comes back and stops on the first short one. Each page is handed to the caller and committed in its own database transaction before the next is requested, so a long sync lands incrementally instead of buffering the whole collection in memory.

The new watermark is only ever safe to persist once every page has been consumed successfully, and it is captured **before** the first request rather than after the last.
Persisting a timestamp captured after fetching would let a write that lands on the server mid-sync fall between the old and new watermark and be skipped forever; capturing it up front means the worst case is a harmless re-fetch of a row already synced, never a permanently missed one.
If any page fails, the whole sync returns an error and the watermark is left untouched, so the next attempt restarts from the last known-good point rather than resuming into a gap.

Each sync also checks the user's `READ` permission for that entity type before issuing a request. A permission failure is terminal rather than retryable, since retrying it would fail identically until the permission itself changes.

### Push-Triggered Entity Sync

A bulk collection sync catches a device up on everything at once, but it only runs when something schedules it. This path covers the complementary case: a single entity changed on another device, and this device should learn about it now rather than at its next full sync.

1. The server sends a push payload whenever an entity is upserted, typed as `PushPayload.EntityNotification` so it carries the `entityType` and `entityUuid` that changed. A plain `PushPayload.Notification` has no entity attached and triggers no sync.
2. The client shows the system notification, then schedules a sync for that one entity.
3. The scheduler runs the job like any other, same chain rules, same connectivity and clock gating, and fetches `GET /api/{feature}/{uuid}`, which returns that single entity rather than a collection.
4. The result is upserted through the same `modifiedAt` guarded path the collection sync uses, so a stale or out-of-order push can never regress a newer local write.

Two properties follow from routing this through the scheduler rather than applying the payload directly. A push that arrives while the device is offline, or whose fetch fails, becomes a persisted job that completes later instead of an update that was simply missed. 
And because the payload is only a *signal* to re-fetch and never the data itself, a push that arrives out of order, duplicated, or with a stale body cannot corrupt local state. The server is still the one asked for the current value.

The follow-up sync is gated by the `pushSync` flag, separately from `push` itself. Disabling it keeps notifications visible while suppressing the background re-fetch each one would otherwise trigger.

# Noteworthy Code

Most of this repository is scaffolding for the app it builds, but a handful of pieces are self-contained enough to be lifted out and published as standalone libraries, 
or dropped into an unrelated Kotlin project with little more than a package rename. 
They are listed here as both a reading guide and a list of extraction candidates.

A shape repeats across most of them, and it is the reason they travel well: 
**an open base class or interface in common code holds the entire state machine, the platform supplies only the primitives it alone can provide, and every platform call is wrapped so a failure degrades instead of crashing.**
A missing capability yields a no-op instance, never an exception.

## LazyData

`shared/src/commonMain/kotlin/com/app/builder/core/flow/LazyData.kt`

Kotlin's `lazy {}` cannot suspend, so anything whose construction needs I/O ends up either built eagerly or hand-guarded at every call site.
`LazyData` closes that gap in a handful of lines: a suspending initializer, a double-checked `Mutex`, and a cached result that is computed exactly once no matter how many coroutines race for it.
It is what lets `Application` and every `TestCase` declare a full dependency graph that is only ever built by the code that actually touches it.
It is a general-purpose `suspend` equivalent of `lazy` that is useful in any coroutine codebase.

## Multiplatform Test Harness

`sharedTest/src/commonMain/kotlin/com/app/builder/test/SharedTestCase.kt` · `PlatformTestCase.kt`

`SharedTestCase` exposes three entry points that share one hermetic setup and teardown: `runUnitTest { }`, `runUITest { }` and `runServerTest { }` - plain coroutines, Compose UI, and a full Ktor server, all from `commonTest`.

Each of them:

- points `Dispatcher.Main`, `Default` and `IO` at the same `UnconfinedTestDispatcher`, so production code, `viewModelScope` included, runs on the test scheduler without any injection ceremony
- resets client and server flags and configs to their defaults
- swaps the telemetry engines for a `MockLogger`
- runs `beforeTest()`, then the test body, then `afterTest()`, cancelling the scope and restoring the dispatchers even when the test fails

The `expect abstract class PlatformTestCase(): SharedTestCase` sitting in between is what makes it portable: the Android actual carries `@RunWith(RobolectricTestRunner::class)` and stubs connectivity, 
the other targets are one-liners, and the test itself never leaves common code.

## Mock Http Engine

`sharedTest/src/commonMain/kotlin/com/app/builder/data/http/MockHttp.kt`

Ktor's stock `MockEngine` declares no capabilities, so any code path that opens a WebSocket, an SSE stream, or sets a timeout cannot be tested against it.
`TestEngine` declares `HttpTimeoutCapability`, `WebSocketCapability`, `WebSocketExtensionsCapability` and `SSECapability`, and re-runs the response adapter that `MockEngine` skips, so typed responses survive the round trip.

On top of it sit `respondMock`, `badRequest`, `notFound`, `respondSse`, and a hand-rolled `respondWebSocket` that fabricates a `WebSocketSession` over channels, emitting a frame followed by a close.
The result is that the entire push stack — WebSocket, SSE and their reconnection loops — is exercised in tests with no server and no sockets.

## Audio Player

`appCore/src/commonMain/kotlin/com/app/builder/core/media/AudioPlayer.kt` · `ui/media/AudioPlayer.kt`

An `open class` holding an `Idle`/`Playing`/`Paused` state machine in common code, driven by four plain methods: `setPlaylist(playlist, loop, shuffle)`, `play()`, `pause()` and `stop()`, plus `dispose()` for teardown. `state` is exposed as a `StateFlow` so the UI observes playback rather than polling it.
Handing it the playlist that is already loaded is a no-op, so recomposition cannot restart the music. Every entry point first checks the `music` feature flag and an `available` hardware flag, falling back to `stop()` when either is off, and wraps its platform call so a failure is reported to `Telemetry` and leaves the player in a valid state rather than throwing.

Platforms override only the protected primitives, never the state machine: ExoPlayer on Android, `AVPlayer` on Apple, JavaFX `MediaPlayer` on Desktop, and an `HTMLAudioElement` on Web.
`rememberAudioPlayer()` is the only entry point a composable needs; it constructs the player and registers a foreground/background lifecycle in the same call, pausing on background and resuming on foreground. 
Construction goes through `AudioPlayer.create()`, which logs and falls back to the inert base class if the platform factory throws, so a device without a usable audio stack degrades to silence instead of taking down the composition.

## Camera

`appCore/src/commonMain/kotlin/com/app/builder/core/media/Camera.kt` · `ui/media/Camera.kt`

An `open class` holding an `Idle`/`Previewing`/`Recording` state machine in common code, exposed as an observable `state: StateFlow`.
Its surface is a set of plain methods covering the full camera workflow: `startPreview()`, `stopPreview()`, `capturePhoto(onResult)`, `startRecording(onResult)`, `stopRecording()`, `toggleFacing()` and `dispose()`.
Captures come back through the `onResult` callback passed at the call site, each carrying the path of the file just written, so a caller receives exactly the capture it asked for.
Every entry point first checks the `camera` feature flag, an `available` hardware flag and a runtime permission check, falling back to `stopPreview()` when any of the three fails, and wraps its platform call so a failure reports to `Telemetry` instead of propagating. 
Illegal transitions are dropped by the base class, so no platform implementation has to re-derive what is valid from where.

Platforms override only the protected `platform*` primitives: CameraX with `ImageCapture` on Android, `AVCaptureSession` on Apple, and `getUserMedia` with a canvas grab on Web.
Desktop is a bare no-op subclass that overrides nothing, so `available` stays `false` and every entry point short-circuits, because there is no standard JVM API for a webcam stream without native libraries. 
Its `CameraPreview` renders a "not supported" placeholder rather than a blank surface, so the degradation is visible in the UI instead of silent.

`rememberCamera()` constructs the camera and registers its lifecycle in one call, falling back to the inert base class if the platform factory throws. 
`CameraPreview` is an `expect` composable, so the preview surface is the only piece of UI that varies.

## NFC

`appCore/src/commonMain/kotlin/com/app/builder/core/nfc/NfcController.kt` · `ui/nfc/NfcController.kt`

An `open class` holding an `Idle`/`Scanning`/`Writing` state machine, driven by `read()`, `write(records)` and `stop()`, each is gated by the `nfc` feature flag and an `available` hardware check, and wraps its platform hook so a failure reports to telemetry and falls back to `Idle` rather than crashing.
Reads don't arrive on the controller's own `state` — they're pushed to a separate module-level `TagProvider.tags` `Flow`, since Android can hand a scanned tag to the app while it's backgrounded, with no live `NfcController` instance around to receive it directly.

A tag carries a list of `NfcRecord`s that the `TagManager` collects off `TagProvider.tags` and interprets into either a Deep Link or a use case.
Underneath, platforms override only the protected `platformRead`, `platformWrite`, `platformStop` and `platformDispose` primitives: Core NFC's `NFCNDEFReaderSession` on Apple; `NfcAdapter` reader mode with NDEF read/write on Android, backed by an `NfcActivity` that catches tag intents when the app isn't foreground and relaunches it.
Desktop and Web are bare no-op subclasses that never override `available`, so it stays `false` and every `read()`/`write()` short-circuits to `stop()`. Neither platform exposes a generic NFC API, and there is no substitute interaction to degrade to, so the honest answer is a controller that reports itself unavailable rather than one that throws.

## Device Location

`appCore/src/commonMain/kotlin/com/app/builder/core/devicelocation/DeviceLocationProvider.kt` · `ui/devicelocation/DeviceLocationProvider.kt`

An `open class` holding an `Idle`/`Active` state machine in common code, driven by `startUpdate()` and `stopUpdate()`, with the most recent fix exposed as a `deviceLocation: StateFlow<DeviceLocation?>`.
Both entry points check the `locationCapture` feature flag, an `available` hardware flag and a runtime permission check, falling back to `stopUpdate()` when any fails, and wrap their platform call so a failure reports to `Telemetry` rather than throwing.

The design decision worth noting is that the update loop exists only to keep the platform's own location cache warm. `getLastKnownLocation()` prefers a fresh platform query and falls back to the last fix the loop captured, so a caller gets an answer immediately instead of waiting on a cold GPS fix, and the running loop is a passive warm-up rather than the delivery mechanism.

`ProvideDeviceLocationProvider()` is the single composable entry point: it provides the instance through `LocalDeviceLocationProvider` and registers a lifecycle that stops capture on background and restarts it on foreground only if it was `Active` before, so backgrounding never silently promotes an idle provider into a running one.

## Cryptography

`appCore/src/commonMain/kotlin/com/app/builder/core/security/Cryptography.kt`

Functions to `encrypt`, `decrypt` and `hash`, backed by real platform keystores rather than a bundled key:
AES-GCM with an Android Keystore key, `CCCrypt` with the key held in the iOS Keychain, an AES-GCM key in a PKCS12 `KeyStore` file under the app data directory on Desktop, and WebCrypto with a non-extractable key in IndexedDB on Web.
Everything is Base64 in and Base64 out, so the caller never handles bytes.
Desktop is the interesting one, because a JVM app has no OS-managed keystore to lean on. Rather than ship a hardcoded password for the `security.p12` file, it derives one from the machine's own identity:
`/var/lib/dbus/machine-id` or `/etc/machine-id` on Linux, `MachineGuid` from the registry on Windows, `IOPlatformUUID` from the I/O Kit registry on macOS. The keystore is therefore bound to the machine that created it. 
Copying the file to another computer yields a keystore that cannot be opened, which is roughly the property the mobile keystores give for free.

In development mode a failure returns the plaintext instead of `null`, which keeps a machine without a usable keystore from bricking the app, while production correctly reports the failure.
This is the layer `StorageFile` sits on to make encryption at rest a per-file boolean.

## Persisted Storage

`appCore/src/commonMain/kotlin/com/app/builder/data/storage/`

A small persistence layer with a set of guarantees, all inside one class:

- **`StorageFile<T>`** declares a file as a typed object: path, serializer, whether it is encrypted, and whether it is `cache`d. `save`/`load` serialize, encrypt and hit the file system in one step.
- Caching is optional and internal, not a separate class: when `cache = true`, `StorageFile` lazily hydrates a mutex-guarded `StateFlow` on first access and every `save` commits to disk before updating it, 
rolling the in-memory value back if the disk write fails, so the cache never advertises a payload the file system rejected. Files read rarely, or written from outside the app can opt out with `cache = false`.

The design decision that ties it together: saving a `null` payload deletes the file.
Absence therefore has a single representation on disk, a cleared file leaves nothing behind, and `reset()` and "save null" converge on the same state.

## Clock Trust Service

`appCore/src/commonMain/kotlin/com/app/builder/domain/clock/`

A service answering one question as a `StateFlow<Boolean>`: can the device clock be trusted? Three signals feed it: 
- An NTP-style offset that the HTTP layer recalibrates on every response and compares against a configured drift tolerance 
- OS-level clock-change notifications that revoke trust the moment the clock is tampered with
- A probe loop that runs only while trust is lost, re-measuring the offset until it comes back within tolerance

It matters because it is a dependency of the offline engine: the scheduler pauses dispatch entirely while trust is lost, rather than letting jobs reach the server with timestamps that would corrupt `modifiedAt` based conflict resolution.
Any offline-first system with last-write-wins semantics needs this and most don't have it.
See [Clock Trust](#clock-trust).

## Push Suite

`appCore/src/commonMain/kotlin/com/app/builder/domain/push/` · `server/src/main/kotlin/com/app/builder/domain/push/`

Three transports: FCM, WebSocket and SSE, presented to the app as one stream of `PushPayload`.

- `PushManager` mints a single-use ticket, opens the WebSocket, and reconnects with exponential backoff, all gated on the internet availability flow so a dead network does not spin the loop.
- `BroadcastManager` opens the server-sent event stream and reconnects it with the same backoff and connectivity gating, without a ticket since the broadcast stream is not user-scoped.
- `PushProvider` is where the channels converge: handlers register once and receive every payload regardless of how it arrived. `showNotification` triggers the platform's native system notification, and a tap on it is turned into a `DeepLink` for the client's navigation layer to collect.

Deduplication is what makes "two independent channels" safe to expose as one stream. `PushProvider` keeps a mutex-guarded `ArrayDeque` of recently handled payload uuids and drops any payload whose uuid it has already seen, evicting the oldest entry once the deque passes `maxHandledPayloads`.
A bounded ring rather than a growing set is the deliberate choice: the window only has to be wide enough to cover the gap between the same payload arriving over FCM and over the WebSocket, and a set that remembered every uuid forever would leak for the lifetime of the process.

On the server, entity-triggered notifications are additionally persisted and only cleared on client acknowledgement, giving that path at-least-once delivery.

The lifecycle policy is per-platform and deliberate. Mobile closes its connections on background and leans on FCM, Web keeps the tab connection open alongside a service worker, and Desktop, which has no FCM client SDK at all, stays connected for the process lifetime.

See [Push Notifications](#push-notifications) for the full mechanism, the delivery rules and their trade-offs.

## Server Instance Scaling

`server/src/main/kotlin/com/app/builder/data/signal/`

The piece that is usually retrofitted painfully once a deployment first scales past one machine: the server holds no state that only a single instance can see.
A client's ticket request and its WebSocket upgrade can land on different instances, and a notification can be sent from an instance holding none of the recipient's sockets, so connection tickets (`TicketTable`) and connection presence (`ConnectionTable`) live in the database rather than in memory.
Any instance can therefore redeem a ticket minted by another, and the decision of whether to skip FCM for an already-connected device is correct globally rather than only for the sockets one instance happens to hold.

`InstanceSignal` abstracts cross-instance wake-ups behind `notify`/`observe`, with a Postgres `LISTEN`/`NOTIFY` implementation that gives each channel its own self-healing subscription connection, 
backed by a separate connection for publishing, and an in-process implementation for local development.
Payloads are explicitly fire-and-forget: anything that must survive delivery belongs in the database.
Full rationale in [Multi-Instance Scaling](#multi-instance-scaling).

## Offline Job Scheduler

`appCore/src/commonMain/kotlin/com/app/builder/domain/scheduler/`

The largest extractable piece: a persistent, fault-tolerant job queue whose scheduling stream is the combination of pending jobs, network availability and clock trust, so it pauses and resumes on its own.
Its distinguishing feature is the chain system. Mutations against the same entity run strictly in order, different entities run in parallel, and a full sync suspends until every pending mutation for that entity type has been pushed, 
which is what stops a pull from overwriting unsynced local work.

`JobStore` and `JobFactory` are interfaces, and a `LocalJobStore` ships for in-memory use, so the engine is not bound to SQLDelight or to this app's domain.
See [Offline Engine](#offline-engine) for the full mechanics.

## Http Stack

`appCore/src/commonMain/kotlin/com/app/builder/data/http/` · `shared/src/commonMain/kotlin/com/app/builder/data/http/`

A Ktor client assembled from small plugins where the interesting behaviour lives in the interceptor: it fails fast when networking is flag-disabled or the device is offline, 
stamps every request with correlation, device and app metadata, converts non-2xx responses into typed exceptions, and performs the round-trip time measurement that feeds the clock trust service.
Around it sit token refresh, content negotiation, encoding, caching, SSE and WebSocket configuration, a typed `URL`/`Header`/`Query` registry shared with the server, and an `HttpResult` sealed type so callers pattern-match instead of catching.

## Feature Flags & Remote Configs

`shared/src/commonMain/kotlin/com/app/builder/core/config/`

Four data classes - client and server flags and configs - each with a `default` instance, an atomic `set { }` and a `reset()`, exposed through a `StateFlow`.
They are shared types, so the server serves exactly what the client consumes, a push payload can trigger a re-sync, the client persists them as encrypted files, and tests snapshot and restore them for free.
Adding a toggle is a single field plus its default; there is no schema, no console and no third-party service in the path.

## Telemetry Hub

`shared/src/commonMain/kotlin/com/app/builder/core/telemetry/`

`Telemetry` is a three-method fan-out (`info`, `error`, `feedback`) over a mutable list of `TelemetryEngine`s, with a native console logger, a `ServerLogger`, a Sentry engine and a mock engine shipping against it.
`Console` is an in-memory circular buffer capped at a fixed size, evicting the oldest line once full, and synchronized through a single-threaded dispatcher rather than a lock, so it stays consistent across concurrent coroutines with no platform-specific primitive. 
It is what backs the in-app log viewer without risking unbounded memory. 
It is why any component can report a failure without knowing where logs go, and why the test harness can capture output by swapping the engine list for a `MockLogger`.

## MVI Store

`appCore/src/commonMain/kotlin/com/app/builder/ui/component/Store.kt`

A `ViewModel` backed `Store<State, Action>` implementing MVI in a single file: actions flow in through `send(action)`, which hands them to a `reducer(state, action)` subclasses override; state flows out through `stateFlow`, with `state` as a non-reactive snapshot; and mutations happen only inside `updateState { }`, which applies them atomically.

Three additions make it worth extracting.
`launch(id, replace)` keys background work by identifier against a registry of active jobs, so a re-triggered load cancels its predecessor instead of racing it, and passing `replace = false` inverts that into "ignore the new request while one is already running": the difference between a search box and a submit button, expressed as one parameter.
`Flow<T>.observe(id)` drives collection off the store's own `subscriptionCount`, so a flow runs only while the UI is actually observing, and pauses on a five-second grace period rather than immediately. That delay is the point: a rotation, a configuration change or a fast navigation away and back never tears down and restarts the collection.
`toStateFlow(started, initialValue)` shares a cold flow into the `viewModelScope` for the cases where the UI should read a stream directly as state.
See [UI Specifics](#ui-specifics).

## Router & Scene Strategies

`appCore/src/commonMain/kotlin/com/app/builder/ui/navigation/`

A thin `Router` interface, exposing `back()` and `navigate(screen, option)`. Its value is the `NavOption` vocabulary, which turns backstack surgery that is normally open-coded per call site into one named strategy: 
`ADD` pushes, `CLEAR` wipes the stack first, `REPLACE_FIRST`/`REPLACE_LAST` remove an existing instance and everything above it before pushing, `SWAP` lifts an existing instance to the top with its state intact, `POP` returns to an existing instance and discards the one passed in, and `IGNORE` pushes only when that screen isn't already on top.
Matching is by screen class rather than equality, so "go back to the task list" works without reconstructing the exact arguments the original destination was created with. 
`Routing` hosts the display, wires the system back gesture and mouse-back button, provides `LocalRouter`, and falls back to a recovery destination when a serialized backstack names a screen that no longer exists, so a stale stack recovers instead of crashing.

The `scene` package supplies scene strategies, which decide how a list of backstack entries becomes what is actually on screen. 
`SplitSceneStrategy` inspects the top two entries and, when both carry the split metadata key, renders them side-by-side in a single two-pane `SplitScene` instead of stacking them. 
The result is that the same navigation code produces a stacked flow on a phone and a list-detail layout on a tablet or desktop window, with the decision made by the strategy rather than by branching inside each screen.

## Translation Pipeline

`design/src/commonMain/kotlin/com/app/builder/data/translation/` · `ui/Translations.kt`

Translations are served by the server, cached reactively on the client, and mirrored into the bundle as a fallback for a cold start with no network.
Because lookups resolve against the cache's state, a language change or a fresh fetch recomposes every string on screen with no restart, and `InjectTranslations` lets previews and tests seed the cache directly.
See [Translations](#translations).

## Permission Manager

`appCore/src/commonMain/kotlin/com/app/builder/ui/permission/`

An `open class` whose base implementation denies everything, an `expect` `rememberPermissionManager()`, and a `Permission` enum.
Platforms that have a permission model override `hasPermission` and `requestPermission`; platforms that don't inherit a truthful "no" instead of a stub that throws, so call sites never branch on target.

The entry point callers actually use is `grantPermission`. It checks the feature flag that owns the permission first, so a disabled feature can never raise a system dialog, then short-circuits if the permission is already held, and only then requests it.
Requests are deduplicated through a mutex-guarded map of in-flight `Deferred`s keyed by permission: if two screens ask for the camera at the same moment, the second awaits the first request rather than triggering a second system prompt: the failure mode that otherwise produces stacked or auto-denied dialogs.
The in-flight requests are held in a `SupervisorJob` scope of the manager's own, so a caller whose composable leaves the screen mid-prompt doesn't cancel the request out from under the other waiters.

## Honourable Mentions

* **`Dispatcher`** (`shared/core/flow`) — mutable `Main`/`Default`/`IO` indirection, which is the single change that lets the whole codebase be driven by a test dispatcher without injecting a scope into every class.
* **`loop`** (`shared/core/platform`) — a suspending interval loop, so polling code stops open-coding `while (true) { ...; delay() }` at every call site and stays cancellable by construction.
* **`platform`** (`shared/core/platform`) — OS, version, brand and model as one data object, used for request headers and platform branching.
* **No-op implementations** — `NoOpRouter`, `NoOpJobFactory`, `NoOpTranslationStore`, `NoOpSqlDriver`, `NoOpHttpClientEngine`, `NoOpFcmService`. A consistent way to keep a subsystem optional without nullability spreading through its callers.
* **`build-logic`** — convention plugins that centralize target configuration and the desktop packaging and notarization pipeline; the root `build.gradle.kts` adds `bumpVersion`, propagating a version across Gradle, Xcode and the server, 
and each module's own `build.gradle.kts` drives its Kover verification off the `@ExcludeFromTesting` annotation instead of Gradle-side path lists.
* **`designShowcase`** — a runnable catalog of every design system component, color, shape and typography style on Android, Desktop and Web, which doubles as the fastest way to review a component across targets.

# FAQ

### How do I set up the project from scratch?

Clone it and run `./gradlew :server:run` followed by a client target. 
Nothing else is required to get a working app: `local.properties` is read only if it exists, and every key it can hold falls back to an empty default, so a missing file costs you signed release builds and Sentry reporting, not a working development environment.

Add a `local.properties` to the project root once you need one of the things it gates:

* Signing an Android release, or signing and notarizing a Mac build.
* Reporting to Sentry from a production client build.
* Web push, which needs a VAPID key and a Firebase web config.

See [Environment Setup](#environment-setup) for the full key list, and [Push Notifications](#push-notifications) if you want FCM working end-to-end.

---

### Do I need Postgres or Firebase to develop locally?

No. `./gradlew :server:run` boots with an in-memory H2 database, an in-process signal, a no-op FCM service, and a seeded admin account. Zero external services. 
See [Development vs Production](#development-vs-production).

---

### Why is dependency injection done manually instead of using a DI framework?

Kotlin Multiplatform support in popular DI frameworks adds complexity and limitations across all target platforms.
The client and the server each own a `Dependency` class that wires their full stack explicitly, which keeps the initialization path transparent and avoids generated code issues particularly on WASM.
On the client it is split in two: `Dependency.AppGraph` holds everything that exists before anyone logs in, and `Dependency.UserGraph` is built per authenticated user on top of it, cached by user uuid, so logging out and back in as someone else swaps the authenticated half without rebuilding the app-wide half.

---

### Can I use the client without the bundled server?

Yes. Implement your own if you like. This is a starter kit. Or run the client fully disconnected in one of two ways: swap `HttpClientEngine` for `NoOpHttpClientEngine`, which answers every request with a `204`, or disable the `ClientFlags.http` flag, which makes outbound requests fail immediately without touching the network.
The difference matters: the no-op engine lets calling code take its success path against empty responses, while the disabled flag exercises the failure path. The offline engine keeps queueing jobs either way, since it pauses on connectivity rather than discarding work.
The client test harness uses the same seam for isolation, substituting `createMockHttpEngine()` so tests get scripted responses instead of a real socket.

---

### Can I add a Splash, Home and Error Screens?

Those are already in `clientApp`'s `ui/screen/` package as placeholder screens (mostly), one sub-package each: `splash`, `home` and `error`.
They are deliberately thin, so replacing one is editing its composable rather than wiring a new destination. 
`App` picks between `LoadingNavigation`, `UnauthenticatedNavigation` and `AuthenticatedNavigation` based on app state, which is what decides when the splash gives way to the rest of the app.

---

### Do I need user and session management?

It is easier to strip down a project than to build authentication and session handling from scratch. 
If you do not need user accounts or sessions, you can remove the corresponding routes and screens or port them to the `UnauthenticatedNavigation`, or give all permissions to the `guest` user in the `Dependency` graph.

---

### Should I share domain models directly with the server, or use separate DTOs?

The `shared` module is the single source of truth for domain models used by both sides.
Models are annotated with `@Serializable` and sent over the wire directly; no separate DTO layer is needed.
The one mapping that does exist is between the domain model and each side's persistence schema, and it is kept local to the feature that owns it: 
on the client in `domain/gateway/{feature}/Mappers.kt`, converting to and from the SQLDelight row, and on the server in the feature's `Exposed` table. The API contract itself stays the domain model, so adding a field is one change in `shared` plus the two mappers that persist it.

---

### How do I handle platform-specific permissions?

Call `grantPermission(permission)` on the `PermissionManager` from `LocalPermissionManager`. 
It is the only entry point you need: it checks the feature flag owning that permission, returns early if the permission is already held, requests it otherwise, and deduplicates concurrent requests so two screens asking at once produce one system prompt rather than two.

Adding a new permission means adding the enum entry and then implementing it in each platform's `PermissionManager` actual. Platforms without a permission model inherit a base implementation that denies everything, so call sites never branch on target, and a platform you haven't implemented yet returns a truthful "no" rather than throwing.
The platform manifests are still yours to maintain: Android needs the entry in `AndroidManifest.xml` and iOS the usage-description key in `Info.plist`, since neither can be declared from common code.

---

### How do I add offline support for a new operation?

Add the entity to `EntityType`, create the corresponding gateway in `domain/gateway` with its `.sq` queries, then handle the new entity in `JobProvider.resolve`, which switches on the job's `EntityType` and `Type` (`GET`, `POST`, `DELETE`) and delegates to the feature's **`Repository`**, not its `UseCases`.
That split is deliberate: the scheduler needs the read/sync surface without the UI-facing write API that would re-enqueue work and loop. See [UseCases VS Repositories](#usecases-vs-repositories).

Once it resolves, the scheduler persists the job, retries it on failure up to `schedulerMaxAttempts`, and pauses dispatch entirely while the device is offline or its clock is untrusted, resuming on its own when both recover.
Write the local database change first and queue the job second, so the UI updates optimistically from the database flow rather than waiting on the network.

---

### How are data sync conflicts handled between the client and server?

The client writes locally first (optimistic write) and queues a background `Job` for the server sync.
When the server response arrives, `modifiedAt` timestamps are compared, and the incoming version is saved only if it is equal to or newer than the local one (`remote.modifiedAt >= local.modifiedAt`).
Batch syncs perform the same comparison in memory across a locally chunked query, then commit only the outdated rows in a single atomic transaction.
Collection fetches are paginated: a fixed `last_sync_utc` watermark bounds the whole sync, while a separate, walking `cursor_utc`/`cursor_uuid` pages through the results newest-first.
Scheduling conflicts are governed by `Job.ConflictPolicy`: typically, `GET` jobs use `IGNORE` (skip if an identical fetch is already pending) and `POST`/`DELETE` jobs use `APPEND` (queue normally, preserving the full operation history).

See [Delta Sync](#delta-sync) for the full mechanism, including the push-triggered single-entity sync path that complements it.

---

### How do I turn a feature off, or roll one out gradually?

Flip its flag. Features are gated by `ClientFlags` on the client and `ServerFlags` on the server, both plain data classes of booleans with a `default` instance, exposed as a `StateFlow` the UI and the routes read live.
The client fetches its flags from the server, persists them as an encrypted file, and hot-reloads them, so flipping a flag server-side takes effect without a release. Turning one off disables the whole vertical slice: the navigation entry, the screen, the background jobs and, on the server side, the routes themselves, which is why a new feature should get its flag before it gets its first screen.

Tunable numbers live alongside them in `ClientConfigs` and `ServerConfigs` rather than as constants, covering things like `clockDriftTolerance`, `schedulerMaxAttempts`, HTTP timeouts, push retry backoff and sync `batchSize`.
See [Feature Flags & Remote Configs](#feature-flags--remote-configs).

---

### How do I add a new translation or language?

Add the entry to the server's `main/resources/static/translations.json`, which is the source of truth the client fetches at runtime, and mirror it into the client's bundled `commonMain/composeResources/files/translations.json` so a cold start with no network still renders it.
Each entry is a `languageIso`/`key`/`value` triple, so a new language is the same keys repeated under a new ISO code rather than a new file.
Read it in a composable with `getTranslation(key, args)`, which resolves against the cache's state, so a language change recomposes every string on screen without a restart.
See [Translations](#translations).

---

### How do I run the tests, and why does the build fail on coverage?

Each module has its own task: `./gradlew clean testSharedAndReport`, `testAppCoreAndReport`, `testClientAndReport` and `testServerAndReport`.

Kover is configured to verify a minimum of **90%** coverage per module, so a run below that fails the build rather than reporting quietly. 
Exclusions are annotation-driven rather than listed in Gradle: mark anything genuinely untestable with `@ExcludeFromTesting` and it drops out of the measurement, alongside generated code, `@Serializable` classes and `@Preview` composables.
Write tests in `commonTest` wherever possible; the harness runs plain coroutines, Compose UI and a full Ktor server from common code. See [Testing](#testing).

---

### Can I drop down to native platform code if KMP lacks a feature?

That is part of the beauty of KMP. How much code is written in Kotlin and how much in platform native code is up to you.

The mechanism is `expect`/`actual`: declare the contract in `commonMain` and implement it in `androidMain`, `appleMain`, `desktopMain` or `webMain`, each of which can call its platform APIs directly. 
Android and Desktop reach the JVM ecosystem, `appleMain` calls UIKit and Core Foundation through the Kotlin/Native interop, and `webMain` drops to JavaScript. 
Several subsystems here already do exactly that; see [Platform Specific Implementations](#platform-specific-implementations) for the full list of declarations that resolve per source set.

The convention worth keeping is the one the existing code follows: put the state machine and the validation in the common `open class`, and let the platform override only the narrow primitives it alone can provide. 
That way a platform that cannot support a feature returns an unavailable instance instead of throwing, and the behaviour stays defined in one place rather than being re-derived four times.

---

### Why does Desktop never use FCM for push notifications?

There is no official Firebase Cloud Messaging client SDK for a JVM desktop app.
Desktop therefore never registers a device token and is never a target for an FCM send, so it relies entirely on its WebSocket and SSE connections, which stay open for the process lifetime rather than closing on background as they do on mobile.

Note that this only removes the *transport*, not the notification. Desktop still implements `NotificationProvider.showNotification`, rendering payloads through the AWT `SystemTray` with a click handler wired back into `PushProvider`, so a payload arriving over the open socket still surfaces as a real system notification.

See [Client Connection Lifecycle](#client-connection-lifecycle).

---

### Why do Android and iOS close their WebSocket/SSE connections in the background instead of keeping them alive?

Two reasons. First, the OS suspends idle sockets shortly after backgrounding anyway (Android Doze, iOS background execution limits), so keeping the app's own connection "alive" while backgrounded is often an illusion.
Second, FCM/APNs already exist specifically to wake up a backgrounded or killed app reliably and battery-efficiently. Reimplementing that with a self-managed socket is worse on every axis (battery, data usage, reliability) for no latency benefit a background push notification needs anyway.
`RegisterPushLifecycle` closes the connection on background and reopens it on foreground on those two platforms only.

---

### Why doesn't the server retry a failed WebSocket send by falling back to FCM for that same notification?

It could, but that would mean computing the FCM-exclusion list before attempting the WebSocket send, then re-sending via FCM only for the subset that failed, coordinating concurrent per-device sends before firing a single batched FCM call.
The current design accepts a narrower gap instead: a WebSocket failure can miss that one notification until the next one re-checks connection presence.
That tradeoff is acceptable because the actual fix for the underlying problem is the client lifecycle policy: a WebSocket only stays open in the first place when the OS says the app is genuinely reachable, so failures are rare and short-lived. See [Client Connection Lifecycle](#client-connection-lifecycle).
If you ever widen the client lifecycle policy (e.g. keeping sockets alive longer in the background on some platform), revisit this tradeoff.

---

### Why are WebSocket connection tickets and connection presence stored in the database instead of an in-memory map?

An in-memory map only exists on the instance that created it. On Cloud Run (or any horizontally-scaled deployment), a client's ticket request and its WebSocket upgrade can land on different instances, and a notification can be sent from an instance that holds none of the recipient's sockets.
Persisting tickets (`TicketTable`) and presence (`ConnectionTable`) makes both instance-agnostic: any instance can redeem a ticket or correctly decide whether to skip FCM for a connected device, regardless of which instance actually holds the socket.

See [Multi-Instance Scaling](#multi-instance-scaling).

---

### Why Postgres LISTEN/NOTIFY instead of Redis or a message broker for cross-instance signaling?

The project already requires Postgres for everything else, so `LISTEN`/`NOTIFY` covers the "wake up whichever instance holds this socket" need without a new piece of infrastructure to run, monitor, and pay for.
It's a deliberately minimal fire-and-forget signal. Anything that must survive a missed delivery (the notification itself, connection presence) is already in the database, not riding on the signal.
If you outgrow this (very high fan-out, cross-region, need for durable ordered delivery), that's when a real message broker earns its keep.

---

### Where do I change the production server host?

`URL.HOST` in `shared/src/commonMain/kotlin/com/app/builder/data/http/URL.kt`. Development builds ignore it and target `URL.DEV_HOST` (`localhost:8080`) instead, so you can leave dev alone while switching production.
If the web app is served through Firebase Hosting, also update the `/api/**` rewrite in `firebase.json`.

---

### Do you use AI?

Autonomously in the code? No. As an assistant to write documentation? Sure. As an advanced search engine? Also yes.

To be clear, I am not against AI in code, but a supporter of a strict boundary between human-authored and AI-generated code, especially in the foundational layer.
This prevents architectural drifts in my experience. Therefore, I strongly advocate for placing AI generated code into completely separate modules, to ensures a clear, undeniable boundary between the human-crafted and AI outputs.

In this project, the foundational code is human authored. Again, it does not mean that AI was not used as a helper to get to a solution or as a productivity multiplier.
I recommend using AI to generate isolated, repeatable features, and their tests, provided they follow the project's established patterns, and are always human reviewed.
Moreover, the design module is a great candidate for AI-assisted development as an isolated design system where all UI components live.

---

# License

Copyright (C) 2024-2026 Vitor Ribeiro.

This project is licensed under the **GNU Affero General Public License v3.0** (see the [LICENSE](LICENSE) file for the full text).

In short: you are free to use, modify, and distribute this project, but derivative works must be distributed under the same license with source available, and you must keep the copyright notice and credit the original author. The Affero clause extends that to network use: if you run a modified version as a hosted or networked service, you must offer its source to the users of that service. If that copyleft requirement doesn't fit your use case, treat the project as a reference architecture instead, or [get in touch](#shameless-plug) about a commercial license.

As the sole copyright holder, I retain the right to license this code under other terms.
