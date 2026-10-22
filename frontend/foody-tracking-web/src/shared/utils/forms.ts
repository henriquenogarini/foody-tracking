import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import type { ApiError } from '@/shared/api/problem'

/**
 * Puts the API's per-field validation messages on the matching form fields (paths already in
 * react-hook-form form, see toApiError). `fields` limits it to inputs the form renders: a list, or a
 * predicate for dynamic paths such as `items.3.unitPrice`.
 * Returns false when nothing was applied, so the caller shows the general message instead.
 */
export function applyFieldErrors<T extends FieldValues>(
  error: ApiError,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[] | ((path: string) => boolean),
): boolean {
  const isRendered =
    typeof fields === 'function' ? fields : (path: string) => fields.some((known) => known === path)
  const entries = Object.entries(error.fieldErrors).filter(([field]) => isRendered(field))
  // The API path is the form path (toApiError converted it), hence the cast.
  // Focusing the first invalid field makes screen readers announce its message (aria-describedby).
  entries.forEach(([field, message], index) =>
    setError(field as Path<T>, { type: 'server', message }, { shouldFocus: index === 0 }),
  )
  return entries.length > 0
}
