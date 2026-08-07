import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        PlatformModuleKt.initKoinIOS()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}