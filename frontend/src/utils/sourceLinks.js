const isLinkedInHost = (hostname) => hostname === 'linkedin.com' || hostname.endsWith('.linkedin.com');

/**
 * Return a canonical LinkedIn URL only when it identifies a specific post.
 * Company feeds, profiles, search pages, and short links are not post sources.
 */
export function getExactLinkedInPostUrl(value) {
  if (typeof value !== 'string' || !value.trim()) return null;

  try {
    const url = new URL(value.trim());
    if (url.protocol !== 'https:' || !isLinkedInHost(url.hostname.toLowerCase())
      || url.username || url.password || url.port) {
      return null;
    }

    const path = url.pathname;
    const isPostSlug = /^\/(?:[a-z]{2}\/)?posts\/[^/]*[-_](?:activity|share|ugcpost)-\d{8,}(?:-[a-z0-9_-]+)?\/?$/i.test(path);
    const isFeedUpdate = /^\/(?:[a-z]{2}\/)?feed\/update\/urn:li:(?:activity|share|ugcpost):\d+\/?$/i.test(path);
    if (!isPostSlug && !isFeedUpdate) return null;

    return `https://www.linkedin.com${path.replace(/\/+$/, '')}`;
  } catch {
    return null;
  }
}

/** Only allow an exact post URL for LinkedIn leads; preserve safe external sources for others. */
export function getLeadSourceUrl(lead) {
  if (lead?.sourcePlatform === 'LINKEDIN') return getExactLinkedInPostUrl(lead.sourceUrl);
  if (typeof lead?.sourceUrl !== 'string' || !lead.sourceUrl.trim()) return null;

  try {
    const url = new URL(lead.sourceUrl.trim());
    return url.protocol === 'https:' ? url.href : null;
  } catch {
    return null;
  }
}
