# Sample result shape

The example below is **synthetic and illustrative only**. It is not a record collected by this project, does not describe a real person or organization, and must not be treated as a verified lead. Angle-bracket values are placeholders. No sample lead is inserted into the application's database.

```json
{
  "sourcePlatform": "LINKEDIN",
  "title": "SAMPLE ONLY — Example creator campaign request",
  "content": "Synthetic example text only: looking for a content creator to support a campaign.",
  "sourceUrl": "[exact LinkedIn post permalink returned by the provider]",
  "author": null,
  "authorProfileUrl": null,
  "company": null,
  "location": null,
  "publishedAt": null,
  "contactInfo": null,
  "searchProviders": "Tavily, Exa",
  "providerMatchCount": 2,
  "serviceCategory": "INFLUENCER_MARKETING",
  "intentLevel": "HIGH",
  "relevanceScore": 72,
  "isIndianMarket": true
}
```

## Reading a real dashboard result

- Open the **Open source** link and confirm that it resolves to the intended LinkedIn post.
- Treat the title/text and date as search-provider output. The text can be partial or stale.
- A blank author, organization, date, or contact field means the provider response did not supply a value that the current adapter could use. The LinkedIn adapters currently do not populate organization or location.
- Treat category, intent, relevance, completeness, and India-market signals as app-generated aids for review, not as facts supplied by LinkedIn.
- If a contact string appears, confirm it belongs to the author or organization before contacting them.

This placeholder documents the API/UI shape without inventing a real result. Live records are returned only when the backend can connect to its configured database and at least one search provider is configured.
