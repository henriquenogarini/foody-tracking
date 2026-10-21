import { readdirSync } from 'node:fs'
import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import prettier from 'eslint-config-prettier/flat'
import { defineConfig, globalIgnores } from 'eslint/config'

// Folder boundaries from CLAUDE.md §3: app → features → shared, features never import each other,
// and cross-folder imports go through the @/ alias. Features are read from disk so new ones are covered.
const features = readdirSync(new URL('./src/features', import.meta.url), { withFileTypes: true })
  .filter((entry) => entry.isDirectory())
  .map((entry) => entry.name)

const noParentImports = {
  regex: '^\\.\\./',
  message:
    'Import across folders with the @/ alias; ./ is only for the same folder (CLAUDE.md §3).',
}

function boundaries(forbidden, message) {
  const patterns = forbidden.length
    ? [noParentImports, { regex: forbidden.join('|'), message }]
    : [noParentImports]
  return { 'no-restricted-imports': ['error', { patterns }] }
}

export default defineConfig([
  globalIgnores(['dist', 'coverage', 'design']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
    },
  },
  { files: ['src/**/*.{ts,tsx}'], rules: boundaries([]) },
  {
    files: ['src/shared/**/*.{ts,tsx}'],
    rules: boundaries(
      ['^@/(app|features)/'],
      'shared/ must not depend on app/ or features/ (CLAUDE.md §3).',
    ),
  },
  ...features.map((feature) => ({
    files: [`src/features/${feature}/**/*.{ts,tsx}`],
    rules: boundaries(
      [
        '^@/app/',
        ...features.filter((other) => other !== feature).map((other) => `^@/features/${other}/`),
      ],
      `features/${feature} must not import app/ or another feature (CLAUDE.md §3).`,
    ),
  })),
  // Must stay last: turns off every stylistic rule that would fight Prettier.
  prettier,
])
