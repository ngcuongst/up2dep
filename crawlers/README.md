# Up2Dep Crawler
We use this crawler to retrieve third-party libraries from 3 sources: maven, jcenter, and Google repo.

The file all-libraries.json contain information of 1879 libraries (group-id, artifact-id). These are needed to crawl all their binary files (including their version history)

# Usage

python3 library_crawler.py  all-libraries.json [download-folder]

where:

- all-libraries.json is where the crawler gets information about libraries (e.g., repo, group-id, artifact-id ...) 
- download-folder is where you want to save the downloaded libraries


# Note

This process takes quite a long time since the crawler will go over 1879 libraries, and download all their version history. To respect the corresponding servers (e.g., maven, jcenter, google), we currently set a waiing time of 30 seconds when a server returns a status of 503 (service unavalable). 
This is an experimental number, you might probably need to adjust this number (variable: SLEEP_WHEN_SERVER_INAVAILABLE) as we are not in control of these servers. 
