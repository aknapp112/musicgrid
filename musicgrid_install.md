This document assumes the user will be using a Windows machine and will require the installation of the following:

1. Python 3.x
2. Java (JRE 6 or better)
3. uTorrent 3.x

Installation Steps:

1. Obtain the MusicGrid bundle and extract to your user’s home directory (ex. C:\Users\Andy)

2. Create a directory under your user’s home directory called .ssh (ex. C:\Users\Andy\.ssh)

3. Copy the known\_hosts file from MusicGridClient\etc to the .ssh directory you just created

4. Create a directory in your user’s home directory called torrenttemp

5. Create a directory in your user’s home directory called torrentwatch



Configuration Steps:


1. Edit the mgc.props file located in MusicGridCient/scripts
update the following settings to match the actual directory on your machine:
> torrent.watch.dir
> torrent.temp.dir
> path.to.createtorrent
> path.to.python

2. Inside uTorrent, go to the Options -> Preferences menu.
3. In Preferences, expand the Advanced menu
4. Under Advanced, select Run Program
5. In the Run Program window, enter the following string, with the directories changed to match your environment,  in the text box labeled: “Run this program when a torrent finishes: “

"c:\MusicGridClient\scripts\MusicGridClient.bat - Shortcut.lnk"  "%N" %T "%D"

6. Click Apply, then click OK.
7. Next we want to add the RSS feed.  Go to File -> Add RSS Feed
8.  In the Add RSS Feed window, cut and paste the following URL:

http://musicgrid.chowned.net/musicman.xml


9. In the Subcription pane, choose either “Do not automatically download all items” or “Automatically download all items published in feed” based on your personal preference.

10. Under Options -> Preferences, go to Directories.

11. Tick “Automatically load .torrents from”, and add the path to your torrentwatch directory (change Andy to your username!):

> C:\Users\Andy\torrentwatch





