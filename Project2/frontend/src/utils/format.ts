export function formatDate(iso: string | null | undefined, emptyLabel = '—') {
  if (!iso) {
    return emptyLabel
  }
  return iso.replace('T', ' ').substring(0, 16)
}

export function truncate(value: string | null | undefined, maxLen: number) {
  if (!value) {
    return ''
  }
  return value.length > maxLen ? `${value.substring(0, maxLen)}...` : value
}
