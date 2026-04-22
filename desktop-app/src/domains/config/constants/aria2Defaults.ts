export const DEFAULT_ARIA2_PROFILE_JSON = `{
  "enable-dht": "true",
  "enable-dht6": "true",
  "bt-enable-lpd": "true",
  "enable-peer-exchange": "true",
  "listen-port": "51413",
  "dht-listen-port": "51413",
  "seed-time": "0",
  "follow-torrent": "true",
  "follow-metalink": "true",
  "allow-overwrite": "true"
}`;

export const DEFAULT_ARIA2_TRACKER_LIST_TEXT = `udp://tracker.opentrackr.org:1337/announce
udp://tracker.torrent.eu.org:451/announce
udp://open.stealth.si:80/announce
udp://tracker.tryhackx.org:6969/announce
udp://tracker.qu.ax:6969/announce
https://tracker.opentrackr.org:443/announce`;
