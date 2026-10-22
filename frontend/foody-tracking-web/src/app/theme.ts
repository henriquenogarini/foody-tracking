import { createTheme } from '@mui/material/styles'

// Design tokens from the Claude Design export (CLAUDE.md §6). Components read them through the
// theme (`sx={{ color: 'brand.700' }}`), never as hex literals.
const brand = {
  50: '#fff4ec',
  100: '#ffe3d1',
  400: '#ff7a2e',
  500: '#f25c05',
  600: '#e65806',
  700: '#a63d04',
  800: '#7a2d03',
}

const ink = {
  300: '#c2b7ae',
  500: '#73665e',
  700: '#4a3f38',
  900: '#1b1410',
}

const surface = {
  0: '#ffffff',
  50: '#f8f5f2',
  100: '#f1ece8',
  200: '#e8e1db',
}

const border = { main: '#e5ddd6', strong: '#8c7e73' }
const danger = '#c62828'

// Keyed by design name; features/orders maps each OrderStatus to one of these.
const status = {
  received: { bg: '#e6edfb', fg: '#2654b8' },
  preparing: { bg: '#fff0d4', fg: '#8a4b00' },
  out: { bg: '#ffe3d1', fg: '#a63d04' },
  delivered: { bg: '#e0f3e8', fg: '#176a3f' },
  cancelled: { bg: '#fde6e4', fg: '#b0251d' },
} satisfies Record<string, { bg: string; fg: string }>

export type StatusToneName = keyof typeof status

declare module '@mui/material/styles' {
  interface Palette {
    brand: typeof brand
    ink: typeof ink
    surface: typeof surface
    border: typeof border
    status: typeof status
  }
  interface PaletteOptions {
    brand?: typeof brand
    ink?: typeof ink
    surface?: typeof surface
    border?: typeof border
    status?: typeof status
  }
}

const fontDisplay = '"Bricolage Grotesque", "Manrope", system-ui, sans-serif'
const fontSans = '"Manrope", system-ui, -apple-system, "Segoe UI", sans-serif'
const display = { fontFamily: fontDisplay, fontWeight: 700, letterSpacing: '-0.015em' }

const shadowCard = '0 1px 2px #1b14100f, 0 6px 16px #1b14100d'
const shadowButton = '0 1px 0 #ffffff33 inset, 0 2px 6px #f25c0540'

export const theme = createTheme({
  palette: {
    primary: { main: brand[500], light: brand[400], dark: brand[600], contrastText: ink[900] },
    success: { main: '#1f8a52' },
    error: { main: danger },
    text: { primary: ink[900], secondary: ink[500] },
    background: { default: surface[50], paper: surface[0] },
    divider: border.main,
    brand,
    ink,
    surface,
    border,
    status,
  },
  shape: { borderRadius: 10 },
  typography: {
    fontFamily: fontSans,
    h1: { ...display, fontSize: 40, lineHeight: '44px' },
    h2: { ...display, fontSize: 32, lineHeight: '38px' },
    h3: { ...display, fontSize: 24, lineHeight: '30px' },
    h4: { ...display, fontWeight: 600, fontSize: 18, lineHeight: '24px', letterSpacing: 0 },
    subtitle1: { fontWeight: 600, fontSize: 16, lineHeight: '22px' },
    subtitle2: { fontWeight: 600, fontSize: 13, lineHeight: '18px' },
    body1: { fontSize: 15, lineHeight: '22px' },
    body2: { fontSize: 13, lineHeight: '18px' },
    caption: { fontWeight: 500, fontSize: 12, lineHeight: '16px' },
    button: { fontWeight: 700, fontSize: 15, lineHeight: '20px', textTransform: 'none' },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          height: 44,
          padding: '0 20px',
          gap: 8,
          whiteSpace: 'nowrap',
          '&.Mui-focusVisible': { outline: `2px solid ${brand[500]}`, outlineOffset: 2 },
        },
        sizeSmall: { height: 36, padding: '0 14px', fontSize: 14 },
        contained: {
          variants: [
            {
              props: { color: 'primary' },
              style: {
                boxShadow: shadowButton,
                '&:hover': { backgroundColor: brand[600], boxShadow: shadowButton },
              },
            },
          ],
        },
        outlined: {
          color: ink[900],
          borderColor: border.strong,
          backgroundColor: surface[0],
          '&:hover': { borderColor: ink[900], backgroundColor: surface[50] },
        },
        text: { color: ink[700] },
      },
    },
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { border: `1px solid ${border.main}`, borderRadius: 14, boxShadow: shadowCard },
      },
    },
    MuiTextField: { defaultProps: { size: 'small' } },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          backgroundColor: surface[0],
          '& .MuiOutlinedInput-notchedOutline': { borderColor: border.main },
          '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: border.strong },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
            borderColor: brand[500],
            borderWidth: 1,
          },
          '&.Mui-focused': { outline: `2px solid ${brand[500]}`, outlineOffset: 1 },
          '&.Mui-error .MuiOutlinedInput-notchedOutline': { borderColor: danger },
          // Design inputs are 44px tall: 22px line + 11px padding on each side.
          variants: [
            {
              props: { size: 'small' },
              // Inputs and selects, not the multiline textarea (it sizes itself by rows). The right
              // padding is left alone on selects, where it makes room for the arrow.
              style: {
                '& .MuiOutlinedInput-input:not(textarea)': {
                  paddingTop: 11,
                  paddingBottom: 11,
                  paddingLeft: 12,
                  lineHeight: '22px',
                  height: 22,
                },
                '& input.MuiOutlinedInput-input': { paddingRight: 12 },
              },
            },
          ],
        },
      },
    },
    MuiLink: {
      defaultProps: { underline: 'hover' },
      styleOverrides: { root: { color: brand[700], fontWeight: 600 } },
    },
    MuiBottomNavigationAction: {
      styleOverrides: {
        root: {
          // The design spreads the tabs evenly; MUI caps each one at 168px by default.
          maxWidth: 'none',
          color: ink[500],
          gap: 4,
          '&.Mui-selected': { color: brand[700] },
        },
        label: {
          fontWeight: 600,
          fontSize: 12,
          lineHeight: '16px',
          '&.Mui-selected': { fontSize: 12 },
        },
      },
    },
  },
})
