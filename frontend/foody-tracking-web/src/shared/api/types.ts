/** Page envelope returned by every paginated endpoint (page is 0-based). */
export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
