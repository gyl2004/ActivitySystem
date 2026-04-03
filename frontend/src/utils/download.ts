export function downloadBlob(data: Blob, filename: string) {
  const blobUrl = window.URL.createObjectURL(data)
  const link = document.createElement('a')
  link.href = blobUrl
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(blobUrl)
}

