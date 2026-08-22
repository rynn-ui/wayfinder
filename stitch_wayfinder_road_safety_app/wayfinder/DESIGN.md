---
name: Wayfinder
colors:
  surface: '#faf9fc'
  surface-dim: '#dadadc'
  surface-bright: '#faf9fc'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f3f6'
  surface-container: '#eeedf0'
  surface-container-high: '#e8e8eb'
  surface-container-highest: '#e3e2e5'
  on-surface: '#1a1c1e'
  on-surface-variant: '#424752'
  inverse-surface: '#2f3033'
  inverse-on-surface: '#f1f0f3'
  outline: '#727783'
  outline-variant: '#c2c6d4'
  surface-tint: '#005db6'
  primary: '#00478d'
  on-primary: '#ffffff'
  primary-container: '#005eb8'
  on-primary-container: '#c8daff'
  inverse-primary: '#a9c7ff'
  secondary: '#535f70'
  on-secondary: '#ffffff'
  secondary-container: '#d7e3f8'
  on-secondary-container: '#596576'
  tertiary: '#94000a'
  on-tertiary: '#ffffff'
  tertiary-container: '#bb1b1b'
  on-tertiary-container: '#ffcec8'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d6e3ff'
  primary-fixed-dim: '#a9c7ff'
  on-primary-fixed: '#001b3d'
  on-primary-fixed-variant: '#00468c'
  secondary-fixed: '#d7e3f8'
  secondary-fixed-dim: '#bbc7db'
  on-secondary-fixed: '#101c2b'
  on-secondary-fixed-variant: '#3c4858'
  tertiary-fixed: '#ffdad5'
  tertiary-fixed-dim: '#ffb4ab'
  on-tertiary-fixed: '#410002'
  on-tertiary-fixed-variant: '#930009'
  background: '#faf9fc'
  on-background: '#1a1c1e'
  surface-variant: '#e3e2e5'
typography:
  display-lg:
    fontFamily: Roboto Flex
    fontSize: 57px
    fontWeight: '400'
    lineHeight: 64px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: Roboto Flex
    fontSize: 32px
    fontWeight: '400'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Roboto Flex
    fontSize: 28px
    fontWeight: '400'
    lineHeight: 36px
  title-lg:
    fontFamily: Roboto Flex
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Roboto Flex
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0.5px
  body-md:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0.25px
  label-lg:
    fontFamily: Roboto Flex
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-sm:
    fontFamily: Roboto Flex
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 8px
  margin-mobile: 16px
  margin-tablet: 24px
  gutter: 8px
  stack-sm: 4px
  stack-md: 8px
  stack-lg: 16px
  section-spacing: 24px
---

## Brand & Style
The design system is rooted in the principles of Google Material 3 (M3), prioritizing utility, safety, and rapid information processing. The brand personality is calm and technical, designed to reassure the user during travel without providing unnecessary cognitive load.

The style is **Corporate / Modern**, specifically following a strict functionalist interpretation. It avoids decorative flourishes in favor of a clean, geometric layout that emphasizes readability at a glance. Every visual element must serve a functional purpose, ensuring the interface remains unobtrusive and professional.

## Colors
The palette is built for high accessibility and professional reliability. 
- **Primary:** A safety-focused, authoritative blue (#005EB8) used for key actions and active states.
- **Secondary:** A muted slate-blue (#535F70) for utility elements and less prominent information.
- **Tertiary:** A standard safety red (#BA1A1A) reserved exclusively for alerts, warnings, and critical road safety information.
- **Neutral:** A range of cool grays used for backgrounds and surfaces to maintain a calm, technical atmosphere.

Both light and dark modes must maintain a minimum contrast ratio of 4.5:1 for standard text and 3:1 for graphical elements to ensure legibility in varying lighting conditions (e.g., direct sunlight or night driving).

## Typography
This design system utilizes **Roboto Flex** for all roles to leverage its variable weight and width capabilities, ensuring maximum legibility across different screen densities. 

The hierarchy is strictly systematic:
- **Display and Headlines** are used for critical status summaries and large-scale navigation cues.
- **Title and Body** styles are optimized for rapid scanning of road conditions and settings.
- **Labels** are used for technical metadata and small UI controls.

For mobile use cases, headlines scale down to ensure they do not wrap excessively, maintaining the structural integrity of the utility-focused layout.

## Layout & Spacing
The layout follows a **Fluid Grid** model based on a strict 8dp increment, consistent with Material 3 standards. 

- **Mobile:** 4-column grid with 16dp side margins.
- **Tablet/Desktop:** 12-column grid with 24dp side margins.

Spacing is used to group related information logically. Small 4dp (stack-sm) gaps are used for connecting labels to inputs, while 16dp (stack-lg) gaps separate distinct functional blocks. Layouts should prioritize vertical stacking to allow for easy one-handed operation and clear glanceability.

## Elevation & Depth
In accordance with M3, depth is primarily conveyed through **Tonal Layers** rather than heavy shadows. 

- **Level 0 (Surface):** The base background color.
- **Level 1 (Container):** Subtle tonal shift (slightly lighter in dark mode, slightly darker in light mode) used for cards and headers.
- **Level 2+:** Reserved for ephemeral elements like menus or dialogs. 

Shadows, if used, are extremely subtle (low-opacity, neutral tint) and only applied to top-level floating elements like Floating Action Buttons (FABs) to indicate interactability.

## Shapes
The shape language is restrained and professional. A "Rounded" (Level 2) approach is used to provide a modern feel without appearing overly casual or "playful."

- **Standard components** (Buttons, Text Fields): 8dp (0.5rem) corner radius.
- **Large containers** (Cards, Bottom Sheets): 16dp (1rem) corner radius.
- **Small elements** (Chips): 8dp (0.5rem) or fully rounded depending on context.

This consistent radius ensures the UI feels cohesive and systematic.

## Components
- **Buttons:** Use M3 "Filled" buttons for primary actions (e.g., "Start Report") and "Outlined" buttons for secondary actions. Use a minimum height of 48dp for touch targets.
- **Chips:** Used for filtering road safety categories (e.g., "Construction," "Accident"). Use the "Filter Chip" style with a clear trailing icon for removal.
- **Lists:** High-density lists with 16dp padding. Each item should have a clear leading icon (e.g., safety symbol) and a primary/secondary text hierarchy.
- **Input Fields:** Use "Filled" text fields with a 1px bottom stroke for high visibility. Labels must always be visible (no disappearing placeholders).
- **Cards:** Use "Outlined" cards with a subtle border (#C4C7C5) to contain grouped information like trip summaries or vehicle health. Avoid heavy shadows or "Elevated" card variants.
- **Status Indicators:** Use persistent top-bar indicators for real-time safety status (Green: Safe, Amber: Caution, Red: Hazard).