export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
  empty: boolean;
}
export interface PaginationParams {
  page: number;
  size: number;
  sortBy: string;
  sortDirection: 'asc' | 'desc';
}
