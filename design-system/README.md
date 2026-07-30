# OGQ Design System v1.1.0

## Package Contents

| Folder | Platform | Description |
|--------|----------|-------------|
| web/ | Web (CSS/SCSS) | ogq-tokens.css, ogq-tokens.scss, ogq-tokens.json |
| react/ | React | 25 components (Button, Input, Modal, DatePicker, Menu, ...) |
| ios/ | iOS (SwiftUI) | OGQTokens.swift (Colors, Typography, Spacing, Radius, Motion) |
| android/ | Android (Compose) | OGQTokens.kt (Color, TextStyle, Dp, Animation tokens) |
| react-native/ | React Native | ogq-tokens.ts (NativeWind compatible) |
| tokens-source/ | All | Raw JSON design tokens |
| figma/ | Design | Tokens Studio compatible JSON |

## Quick Start

### Web (CSS)
```html
<link rel="stylesheet" href="web/ogq-tokens.css">
<h1 class="ogq-text-display-lg">Hello OGQ</h1>
```

### React
```tsx
import { OGQButton, OGQInput, OGQModal, OGQDatePicker } from '@ogqcorp/design-system'
```

### iOS (SwiftUI)
```swift
Text("Hello").ogqFont(OGQTypography.headingLG).foregroundColor(OGQColors.primary600)
```

### Android (Jetpack Compose)
```kotlin
Text("Hello", style = OGQTypography.headingLG, color = OGQColors.primary600)
```

### React Native
```typescript
import { colors, typography, spacing } from '@ogqcorp/design-system/rn'
```

### Figma
Import `figma/ogq-figma-tokens.json` into Tokens Studio plugin.

## Documentation
https://ogqdesign.itshin.com

## License
Proprietary — OGQ Corp.
