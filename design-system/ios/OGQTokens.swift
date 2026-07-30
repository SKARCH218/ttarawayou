// OGQ Design System — Swift Tokens (SwiftUI / UIKit)
// Auto-generated from OGQ Design System v1.0.0
// Do not edit manually.

import SwiftUI

// MARK: - Colors

public struct OGQColors {
    // Mono
    public static let mono000 = Color(hex: "#ffffff")
    public static let mono010 = Color(hex: "#fcfdfd")
    public static let mono030 = Color(hex: "#f9fbfb")
    public static let mono050 = Color(hex: "#f4f6f6")
    public static let mono080 = Color(hex: "#eef1f1")
    public static let mono100 = Color(hex: "#d8dfdf")
    public static let mono200 = Color(hex: "#a7b6b9")
    public static let mono300 = Color(hex: "#89989c")
    public static let mono400 = Color(hex: "#7d8d92")
    public static let mono500 = Color(hex: "#76888f")
    public static let mono600 = Color(hex: "#6c7e84")
    public static let mono700 = Color(hex: "#57676b")
    public static let mono800 = Color(hex: "#425052")
    public static let mono900 = Color(hex: "#262d2e")
    public static let mono990 = Color(hex: "#262d2e")

    // Primary (Teal)
    public static let primary010 = Color(hex: "#fbfdfc")
    public static let primary030 = Color(hex: "#eef7f4")
    public static let primary050 = Color(hex: "#dff4ea")
    public static let primary080 = Color(hex: "#cbecdf")
    public static let primary100 = Color(hex: "#b5e5d7")
    public static let primary200 = Color(hex: "#9bdfcc")
    public static let primary300 = Color(hex: "#7fdec1")
    public static let primary400 = Color(hex: "#58dab1")
    public static let primary500 = Color(hex: "#32d29d")
    public static let primary600 = Color(hex: "#00c389")
    public static let primary700 = Color(hex: "#00b57f")
    public static let primary800 = Color(hex: "#00996e")
    public static let primary900 = Color(hex: "#007a58")
    public static let primary990 = Color(hex: "#006147")

    // Secondary (Purple)
    public static let secondary010 = Color(hex: "#fbfbfe")
    public static let secondary030 = Color(hex: "#f8f6fe")
    public static let secondary050 = Color(hex: "#ebe8fc")
    public static let secondary080 = Color(hex: "#e0dafb")
    public static let secondary100 = Color(hex: "#d2c8f9")
    public static let secondary200 = Color(hex: "#bfb0f2")
    public static let secondary300 = Color(hex: "#a793ec")
    public static let secondary400 = Color(hex: "#947be5")
    public static let secondary500 = Color(hex: "#7e5ae2")
    public static let secondary600 = Color(hex: "#703fe4")
    public static let secondary700 = Color(hex: "#5b1dcd")
    public static let secondary800 = Color(hex: "#4917a6")
    public static let secondary900 = Color(hex: "#37117e")
    public static let secondary990 = Color(hex: "#290d5e")

    // Semantic
    public static let successLight = Color(hex: "#dff4ea")
    public static let success = Color(hex: "#00c389")
    public static let successDark = Color(hex: "#007a58")
    public static let warningLight = Color(hex: "#fff3e0")
    public static let warning = Color(hex: "#ff9800")
    public static let warningDark = Color(hex: "#e65100")
    public static let errorLight = Color(hex: "#ffe2e4")
    public static let error = Color(hex: "#e21235")
    public static let errorDark = Color(hex: "#c90e2e")
    public static let infoLight = Color(hex: "#e6f4ff")
    public static let info = Color(hex: "#1384d7")
    public static let infoDark = Color(hex: "#0d5fa3")

    // Accent
    public static let green = Color(hex: "#00ff85")
    public static let orange = Color(hex: "#ff550d")
    public static let purple = Color(hex: "#7f00ff")

    // Social
    public static let naver = Color(hex: "#03c75a")
    public static let facebook = Color(hex: "#3d5a98")
    public static let afreeca = Color(hex: "#0545b1")
    public static let google = Color(hex: "#4285f4")
    public static let apple = Color(hex: "#000000")
    public static let kakao = Color(hex: "#fee500")
}

// MARK: - Typography

public struct OGQTypography {
    public struct Scale {
        let fontSize: CGFloat
        let lineHeight: CGFloat
        let letterSpacing: CGFloat
        let weight: Font.Weight
    }

    public static let displayXL = Scale(fontSize: 56, lineHeight: 61.6, letterSpacing: -1.4, weight: .bold)
    public static let displayLG = Scale(fontSize: 48, lineHeight: 55.2, letterSpacing: -0.96, weight: .bold)
    public static let displayMD = Scale(fontSize: 36, lineHeight: 43.2, letterSpacing: -0.54, weight: .semibold)
    public static let headingXL = Scale(fontSize: 30, lineHeight: 37.5, letterSpacing: -0.3, weight: .semibold)
    public static let headingLG = Scale(fontSize: 24, lineHeight: 31.2, letterSpacing: -0.12, weight: .semibold)
    public static let headingMD = Scale(fontSize: 20, lineHeight: 28, letterSpacing: 0, weight: .semibold)
    public static let headingSM = Scale(fontSize: 18, lineHeight: 25.2, letterSpacing: 0, weight: .medium)
    public static let bodyLG = Scale(fontSize: 18, lineHeight: 28.8, letterSpacing: 0, weight: .regular)
    public static let bodyMD = Scale(fontSize: 16, lineHeight: 25.6, letterSpacing: 0, weight: .regular)
    public static let bodySM = Scale(fontSize: 14, lineHeight: 21, letterSpacing: 0.07, weight: .regular)
    public static let caption = Scale(fontSize: 12, lineHeight: 18, letterSpacing: 0.12, weight: .regular)
    public static let overline = Scale(fontSize: 11, lineHeight: 16.5, letterSpacing: 0.88, weight: .semibold)

    // OGQ Original Mixin Scales
    public static let h1 = Scale(fontSize: 56, lineHeight: 56, letterSpacing: 0, weight: .bold)
    public static let h2 = Scale(fontSize: 48, lineHeight: 48, letterSpacing: 0, weight: .bold)
    public static let h3 = Scale(fontSize: 44, lineHeight: 44, letterSpacing: 0, weight: .bold)
    public static let h4 = Scale(fontSize: 40, lineHeight: 40, letterSpacing: 0, weight: .bold)
    public static let h5 = Scale(fontSize: 36, lineHeight: 36, letterSpacing: 0, weight: .bold)
    public static let h6 = Scale(fontSize: 32, lineHeight: 32, letterSpacing: 0, weight: .bold)
    public static let h7 = Scale(fontSize: 28, lineHeight: 28, letterSpacing: 0, weight: .bold)
    public static let h8 = Scale(fontSize: 24, lineHeight: 24, letterSpacing: 0, weight: .bold)
    public static let b1 = Scale(fontSize: 20, lineHeight: 20, letterSpacing: 0, weight: .regular)
    public static let b2 = Scale(fontSize: 18, lineHeight: 18, letterSpacing: 0, weight: .regular)
    public static let b3 = Scale(fontSize: 16, lineHeight: 16, letterSpacing: 0, weight: .regular)
    public static let b4 = Scale(fontSize: 14, lineHeight: 14, letterSpacing: 0, weight: .regular)
    public static let b5 = Scale(fontSize: 12, lineHeight: 12, letterSpacing: 0, weight: .regular)
    public static let c1 = Scale(fontSize: 11, lineHeight: 12, letterSpacing: 0, weight: .regular)
    public static let c2 = Scale(fontSize: 10, lineHeight: 10, letterSpacing: 0, weight: .regular)
}

// MARK: - Spacing

public struct OGQSpacing {
    public static let s0: CGFloat = 0
    public static let s2: CGFloat = 2
    public static let s4: CGFloat = 4
    public static let s6: CGFloat = 6
    public static let s8: CGFloat = 8
    public static let s10: CGFloat = 10
    public static let s12: CGFloat = 12
    public static let s16: CGFloat = 16
    public static let s20: CGFloat = 20
    public static let s24: CGFloat = 24
    public static let s28: CGFloat = 28
    public static let s32: CGFloat = 32
    public static let s40: CGFloat = 40
    public static let s48: CGFloat = 48
    public static let s64: CGFloat = 64
    public static let s80: CGFloat = 80
    public static let s120: CGFloat = 120
}

// MARK: - Border Radius

public struct OGQRadius {
    public static let none: CGFloat = 0
    public static let sm: CGFloat = 4
    public static let r6: CGFloat = 6
    public static let md: CGFloat = 8
    public static let lg: CGFloat = 12
    public static let xl: CGFloat = 16
    public static let xxl: CGFloat = 20
    public static let full: CGFloat = 1000
}

// MARK: - Shadows

public struct OGQShadow {
    public static func xs() -> some View { EmptyView() }
    // Use .shadow(color: .black.opacity(0.05), radius: 1, y: 1)
    // Use .shadow(color: .black.opacity(0.1), radius: 3, y: 1) for sm
    // Use .shadow(color: .black.opacity(0.1), radius: 6, x: 0, y: 4) for md
    // Use .shadow(color: .black.opacity(0.1), radius: 15, x: 0, y: 10) for lg
    // Use .shadow(color: .black.opacity(0.1), radius: 25, x: 0, y: 20) for xl
}

// MARK: - Motion

public struct OGQMotion {
    public static let instant: Double = 0.05
    public static let fast: Double = 0.1
    public static let normal: Double = 0.2
    public static let moderate: Double = 0.3
    public static let slow: Double = 0.4
    public static let slower: Double = 0.5
    public static let slowest: Double = 0.7
}

// MARK: - Z-Index (Reference)

public struct OGQZIndex {
    public static let base: Double = 0
    public static let raised: Double = 1
    public static let dropdown: Double = 1000
    public static let sticky: Double = 1100
    public static let overlay: Double = 1200
    public static let modal: Double = 1300
    public static let popover: Double = 1400
    public static let toast: Double = 1500
    public static let tooltip: Double = 1600
}

// MARK: - Color Hex Extension

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 6:
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8:
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }
        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue: Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

// MARK: - Font Extension

extension View {
    func ogqFont(_ scale: OGQTypography.Scale) -> some View {
        self
            .font(.system(size: scale.fontSize, weight: scale.weight))
            .lineSpacing(scale.lineHeight - scale.fontSize)
            .tracking(scale.letterSpacing)
    }
}
