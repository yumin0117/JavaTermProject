package kr.ac.yumin.playlist.model;

import java.util.List;

public record PlaylistDetail(Playlist playlist, List<Song> songs) {}
