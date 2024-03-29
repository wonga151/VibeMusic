package com.develop.awong.musicplayer2;

import android.Manifest;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.test.mock.MockPackageManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MenuActivity extends AppCompatActivity {
    private static List<String> listTitles;   //for display purpose
    protected static List<String> listFileNames;
    protected static List<String> albumList;
    protected static List<String> currentTrackList;
    protected static HashMap<String, Album> albumMap;
    public static HashMap<String, Song> songMap;  //Map that stores all song objects
    protected static MusicPlayer musicPlayer;

    private static final int REQUEST_CODE_PERMISSION = 2;
    private static String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String TAG = "MenuActivity";

    protected static GPSTracker gps;

    protected static boolean flag = false;
    protected static FirebaseManager firebaseManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        firebaseManager = new FirebaseManager();


        try {
            if (ActivityCompat.checkSelfPermission(this, mPermission)
                    != MockPackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{mPermission}, REQUEST_CODE_PERMISSION);

                // If any permission above not allowed by user, this condition will
                //execute every time, else your else part will work
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "onCreate: User deny permission for location acess!");
        }

        Button albums = (Button) findViewById(R.id.albums);
        Button songs = (Button) findViewById(R.id.songs);
        Button time = (Button) findViewById(R.id.timeBtn);
        Button currTracks = (Button) findViewById(R.id.tracklist);
        Button loginButton = (Button) findViewById(R.id.login_button);
        ToggleButton flashback = (ToggleButton) findViewById(R.id.flashback);
        final ToggleButton vibe = (ToggleButton) findViewById(R.id.vibe);

        Button dl = (Button) findViewById(R.id.download);

        dl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Download download = new Download(MenuActivity.this);
                EditText link = (EditText) findViewById(R.id.urllink);
                EditText songName = (EditText) findViewById(R.id.SongName);

                long songID = download.DownloadData(Uri.parse(link.getText().toString()),MenuActivity.this,songName.getText().toString());
                if (songID == 0) {
                    Toast toast = Toast.makeText(MenuActivity.this,"Invalid URL", Toast.LENGTH_LONG);
                    toast.show();
                }
                //String message = download.DownloadStatus( ,songID);
                /*Toast toast = Toast.makeText(context,
                        message,
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 25, 400);
                toast.show();*/
            }
        });


        //Asking permission for Location services.
        try {
            if (ActivityCompat.checkSelfPermission(this, mPermission)
                    != MockPackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{mPermission}, REQUEST_CODE_PERMISSION);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "onCreate: User denies permission");
        }

        time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MenuActivity.this, CalendarActivity.class);
                Log.d(TAG, "onClick: User clicks the SET TIME button");
                MenuActivity.this.startActivity(i);
            }
        });

        songs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onCreate: User deny permission for location access!");
                Intent i = new Intent(MenuActivity.this, MainActivity.class);
                Log.d(TAG, "onClick: User clicks the SONGS button");
                MenuActivity.this.startActivity(i);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MenuActivity.this, LoginActivity.class);
                Log.d(TAG, "onClick: User clicks the Back to Login button");
                MenuActivity.this.startActivity(i);
            }
        });

        albums.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MenuActivity.this, AlbumActivity.class);
                Log.d(TAG, "onClick: User clicks the ALBUMS button");
                MenuActivity.this.startActivity(i);
            }
        });

        currTracks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MenuActivity.this, TrackListActivity.class);
                Log.d(TAG, "onClick: User clicks the current track list button");
                MenuActivity.this.startActivity(i);
            }
        });

        flashback.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Log.d(TAG, "onClick: User clicks the FLASHBACK button");
                if (b) {  //Flashback mode
                    gps = new GPSTracker(MenuActivity.this);

                    ModePlayer fbp = new FlashbackPlayer(gps.getLocation(), songMap);
                    //populate the flashback playlist

                    //fbp.populatePlaylist();
                    PriorityQueue<Song> playlist = fbp.getPlaylist();

                    fbp.populatePlaylist();

                    if (playlist.isEmpty()) {
                        Log.d(TAG, "onCheckedChanged: Flashback playlist is empty!");
                        Toast.makeText(MenuActivity.this, "No songs have ever been played here before!", Toast.LENGTH_SHORT).show();
                    } else {
                        List<String> listOfSongs = new ArrayList<String>();
                        System.out.println("playlist length :   " + playlist.size());
                        while (!playlist.isEmpty()) {
                            Song temp = playlist.poll();
                            listOfSongs.add(temp.getSongTitle());
                        }

                        System.out.println("list of songs length:   " + listOfSongs.size());
                        ListPlayer lp = new ListPlayer(listOfSongs, MenuActivity.this);
                        lp.playCurrentList();
                        Intent i = new Intent(MenuActivity.this, MusicActivity.class);
                        i.putExtra("songName", musicPlayer.getSongTitle());
                        MenuActivity.this.startActivity(i);
                    }

                } else {  //Normal mode
                    //destroy the musicplayer
                    if (musicPlayer.getMediaPlayer() != null && musicPlayer.getMediaPlayer().isPlaying()) {
                        musicPlayer.getMediaPlayer().stop();
                    }
                }
            }
        });

        vibe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Log.d(TAG, "onClick: User clicks the VIBE button");
                if (b) {  //Vibe mode





                    gps = new GPSTracker(MenuActivity.this);


                    ModePlayer vibePlayer = new VibePlayer(gps.getLocation(), MenuActivity.this);

                    firebaseManager.retrieve(vibePlayer);


                    /*
                    //populate the vibe playlist

                    //vp.populatePlaylist();
                    PriorityQueue<Song> playlist = vp.getPlaylist();


                    vp.populatePlaylist();

                    if (playlist.isEmpty()) {
                        Log.d(TAG, "onCheckedChanged: Vibe playlist is empty!");
                        Toast.makeText(MenuActivity.this, "No songs have ever been played here before!", Toast.LENGTH_SHORT).show();
                    } else {
                        List<String> listOfSongs = new ArrayList<String>();
                        Log.d(TAG, "PlayList length: " + playlist.size());
                        while (!playlist.isEmpty()) {
                            Song temp = playlist.poll();
                            listOfSongs.add(temp.getSongTitle());
                        }

                        System.out.println("list of songs length:   " + listOfSongs.size());
                        ListPlayer lp = new ListPlayer(listOfSongs, MenuActivity.this);
                        lp.playCurrentList();
                        currentTrackList = listOfSongs;
                        Intent i = new Intent(MenuActivity.this, MusicActivity.class);
                        i.putExtra("songName", musicPlayer.getSongTitle());
                        MenuActivity.this.startActivity(i);
                    }
                    */

                } else {  //Normal mode
                    //destroy the musicplayer
                    if (musicPlayer.getMediaPlayer() != null && musicPlayer.getMediaPlayer().isPlaying()) {
                        musicPlayer.getMediaPlayer().stop();
                    }
                }
            }
        });


        //Flag so that we don't recreate songs if we already have before
        //and so that the songs favorite status is preserved.
        if (!flag) {
            musicPlayer = new MusicPlayer();
            listTitles = new ArrayList<>();
            listFileNames = new ArrayList<>();
            albumList = new ArrayList<>();
            albumMap = new HashMap<>();
            songMap = new HashMap<>();
            currentTrackList = new ArrayList<>();

            Field[] fields = R.raw.class.getFields();
            for (int i = 0; i < fields.length; i++) {

                String fileName = fields[i].getName();
                listFileNames.add(fileName);

                // Get albumName and songTitle
                int song_id = this.getResources().getIdentifier(fileName, "raw", this.getPackageName());
                final AssetFileDescriptor afd = getResources().openRawResourceFd(song_id);
                MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                metaRetriever.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                String albumName = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                String songTitle = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

                if(artist == null) {
                    artist = " ";
                }

                byte[] albumArt = metaRetriever.getEmbeddedPicture();


                //Populating albums and songs in albums
                if (!albumList.contains(albumName)) {
                    albumList.add(albumName);
                    Album newAlbum = new Album(albumName);
                    newAlbum.addSong(fileName);
                    newAlbum.addSongTitle(songTitle);
                    albumMap.put(albumName, newAlbum);
                } else {
                    albumMap.get(albumName).addSong(fileName);
                    albumMap.get(albumName).addSongTitle(songTitle);
                }

                //Populating songs in song map
                Song newSong = new Song(fileName, songTitle, albumName, artist, albumArt,
                        LoginActivity.personName, LoginActivity.personEmail, null, null);
                songMap.put(songTitle, newSong);


                flag = true;
                listTitles.add(songTitle);
            }
        }

        Button p = (Button) findViewById(R.id.playBtn);

        p.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText songName = (EditText) findViewById(R.id.SongName);
                Log.d(TAG,getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
                File[] files = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).listFiles();

                Log.d(TAG,"File size is " + files.length);
                for (File f : files){
                    String pa = "file name is " + f.getName();
                    Log.d(TAG,pa);
                }

                String path = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/So.mp3";
                Uri temp = Uri.parse(path);
                MediaPlayer mediaPlayer = MediaPlayer.create(MenuActivity.this, temp);
                mediaPlayer.start();
            }
        });

    }

    public static void extractAlbum(File zipFile, File targetDirectory) throws IOException {
        ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)));
        try {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(targetDirectory, ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                FileOutputStream fout = new FileOutputStream(file);
                try {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                } finally {
                    fout.close();
                }
                if (file.getName().endsWith(".mp3")) {
                    //extract metadata
                    Song temp = new Song();
                }
            /* if time should be restored as well
            long time = ze.getTime();
            if (time > 0)
                file.setLastModified(time);
            */
            }
        } finally {
            zis.close();
        }
    }

    public static Map getSongMap() {
        return songMap;
    }


    public static List<String> getlistTitles() {
        return listTitles;
    }

}
