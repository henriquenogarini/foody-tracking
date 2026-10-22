import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import type { ApiError } from '@/shared/api/problem'

/** Returns false when nothing was applied, so the caller shows the general message instead. */
export function applyFieldErrors<T extends FieldValues>(
  error: ApiError,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[] | ((path: string) => boolean),
): boolean {
  const isRendered =
    typeof fields === 'function' ? fields : (path: string) => fields.some((known) => known === path)
  const entries = Object.entries(error.fieldErrors).filter(([field]) => isRendered(field))
  // Focusing the first invalid field makes screen readers announce its message.
  entries.forEach(([field, message], index) =>
    setError(field as Path<T>, { type: 'server', message }, { shouldFocus: index === 0 }),
  )
  return entries.length > 0
}
