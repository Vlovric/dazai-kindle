---
name: Scholarly Precision
colors:
  surface: '#f8f9ff'
  surface-dim: '#ccdbf4'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e6eeff'
  surface-container-high: '#dde9ff'
  surface-container-highest: '#d5e3fd'
  on-surface: '#0d1c2f'
  on-surface-variant: '#42474b'
  inverse-surface: '#233144'
  inverse-on-surface: '#ebf1ff'
  outline: '#72787c'
  outline-variant: '#c2c7cc'
  surface-tint: '#446274'
  primary: '#002434'
  on-primary: '#ffffff'
  primary-container: '#1a3a4a'
  on-primary-container: '#85a4b7'
  inverse-primary: '#abcbdf'
  secondary: '#505f76'
  on-secondary: '#ffffff'
  secondary-container: '#d0e1fb'
  on-secondary-container: '#54647a'
  tertiary: '#1e2224'
  on-tertiary: '#ffffff'
  tertiary-container: '#343739'
  on-tertiary-container: '#9da0a2'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#c7e7fc'
  primary-fixed-dim: '#abcbdf'
  on-primary-fixed: '#001e2c'
  on-primary-fixed-variant: '#2c4a5b'
  secondary-fixed: '#d3e4fe'
  secondary-fixed-dim: '#b7c8e1'
  on-secondary-fixed: '#0b1c30'
  on-secondary-fixed-variant: '#38485d'
  tertiary-fixed: '#e0e3e5'
  tertiary-fixed-dim: '#c4c7c9'
  on-tertiary-fixed: '#191c1e'
  on-tertiary-fixed-variant: '#444749'
  background: '#f8f9ff'
  on-background: '#0d1c2f'
  surface-variant: '#d5e3fd'
typography:
  display-lg:
    fontFamily: Source Serif 4
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Source Serif 4
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Source Serif 4
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-md:
    fontFamily: Source Serif 4
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Hanken Grotesk
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Hanken Grotesk
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 40px
  2xl: 64px
  gutter: 24px
  margin-mobile: 16px
  margin-desktop: 48px
  max-width-content: 1280px
---

## Brand & Style

The brand personality is intellectual, organized, and quietly authoritative. It targets researchers, students, and avid readers who need to transform raw highlights into structured knowledge. The UI evokes the feeling of a modern library or a focused digital study: calm, distraction-free, and meticulously ordered.

This design system utilizes a **Modern Minimalist** style with **Tonal Layering**. It prioritizes extreme clarity and generous whitespace to reduce cognitive load during intensive parsing tasks. The aesthetic avoids unnecessary flourishes, relying on high-quality typography and a restrained palette to create a "workspace" atmosphere that feels both professional and academic.

## Colors

The color strategy is rooted in "Paper and Ink" metaphors. 

- **Primary (#1A3A4A):** A deep, bookish Dark Teal used for primary actions, active states, and branding elements. It provides a sense of stability and depth.
- **Secondary (#64748B):** A muted Slate Gray for meta-data, secondary labels, and supporting iconography.
- **Neutral/Text (#334155):** A soft charcoal for body text, ensuring high legibility without the harshness of pure black.
- **Background/Surface (#F8FAFC):** An off-white "Ghost White" that mimics high-quality archival paper, reducing eye strain during long reading sessions.
- **Accent/Highlight (#E2E8F0):** Used for subtle borders and structural dividers to maintain a clean grid.

## Typography

The typography system pairs a scholarly serif with a precise, modern sans-serif to balance tradition with utility.

- **Headings:** Source Serif 4 is used for all editorial content, book titles, and page headers. It provides an authoritative, bookish feel that signals a focus on reading and text.
- **UI & Interface:** Hanken Grotesk is used for all functional elements, buttons, and navigation. Its clean, geometric construction ensures that the "tool" aspects of the interface remain distinct from the "content" aspects.
- **Hierarchy:** Use tight letter-spacing for large serif displays to maintain a premium feel. Small labels should use a slightly heavier weight (Medium/SemiBold) to ensure legibility against light backgrounds.

## Layout & Spacing

This design system uses a **Fixed Grid** approach for the central workspace to maintain focus, transitioning to a **Fluid Grid** for library views.

- **Desktop (1440px+):** 12-column grid with 24px gutters. The central reading/parsing area is constrained to 8 columns (approx. 800px) to maintain optimal line lengths for reading.
- **Tablet (768px - 1024px):** 8-column grid with 20px gutters. Sidebars become collapsible drawers.
- **Mobile (<768px):** 4-column grid with 16px margins. Content stacks vertically; large book titles scale down using defined mobile typography tokens.
- **Rhythm:** All spacing is based on a 4px baseline. Components use 16px (md) internal padding as a standard, with 24px (lg) used to separate distinct content blocks.

## Elevation & Depth

To maintain a clean, scholarly aesthetic, depth is communicated through **Tonal Layers** and **Low-Contrast Outlines** rather than heavy shadows.

- **Level 0 (Background):** The base layer using the primary off-white surface.
- **Level 1 (Cards/Sidebar):** Pure white surfaces with a 1px solid border (#E2E8F0). No shadow. This is the primary container for book cards and note previews.
- **Level 2 (Dropdowns/Modals):** Pure white surface with a very soft, ambient shadow (0px 4px 12px, 5% opacity black).
- **Active State:** Elements being dragged or selected gain a subtle "inner glow" or a slightly thicker 2px border in the Primary Teal color.

## Shapes

The shape language is **Soft** and precise. It avoids the playfulness of fully rounded "pill" shapes in favor of professional, architectural corners.

- **Standard Elements:** Buttons, input fields, and small cards use a 0.25rem (4px) radius.
- **Large Containers:** Library book covers and main workspace panels use a 0.5rem (8px) radius.
- **Interactive Indicators:** Mode selectors and tab indicators use sharp or very slightly rounded edges (2px) to emphasize the "utility tool" nature of the application.

## Components

- **Buttons:** Primary buttons are solid Primary Teal with white text. Secondary buttons are outlined in Slate Gray. Use Hanken Grotesk Medium for all button labels.
- **Library Cards:** Vertical orientation. Features a subtle 1px border. The book title uses Source Serif 4, while the author and "Note Count" use Hanken Grotesk in Secondary Slate.
- **File Upload Zone:** A large, dashed-border container (#CBD5E1) with a centered icon. Uses a "drag-and-drop" active state where the background shifts to a very faint tint of the Primary Teal.
- **Mode Selection:** A segmented control (Toggle) with a flat background. The active state is indicated by a white "raised" segment with a subtle 1px border.
- **Note Highlights:** Displayed as blocks with a thick 4px vertical accent bar on the left in Primary Teal. This mimics the look of a traditional margin highlight.
- **Input Fields:** Clean, minimal fields with 1px borders. On focus, the border transitions to Primary Teal with a subtle 2px inset ring.