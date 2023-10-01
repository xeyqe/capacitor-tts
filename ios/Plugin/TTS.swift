import Foundation

@objc public class TTS: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
