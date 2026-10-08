package com.equipe1.aurora.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> userName = new MutableLiveData<>();
    private final MutableLiveData<List<UserCircle>> userCircles = new MutableLiveData<>();
    private final MutableLiveData<List<Friend>> nearbyFriends = new MutableLiveData<>();

    public HomeViewModel() {
        loadUserData();
        loadUserCircles();
        loadNearbyFriends();
    }

    public LiveData<String> getUserName() {
        return userName;
    }

    public LiveData<List<UserCircle>> getUserCircles() {
        return userCircles;
    }

    public LiveData<List<Friend>> getNearbyFriends() {
        return nearbyFriends;
    }

    private void loadUserData() {
        // Simulação de carregamento via Repository / Firebase
        userName.setValue("Alex");
    }

    private void loadUserCircles() {
        // Lista de círculos de segurança ativos do usuário
        List<UserCircle> circles = new ArrayList<>();
        circles.add(new UserCircle("Família", "4 membros ativos"));
        circles.add(new UserCircle("Faculdade", "6 membros ativos"));
        circles.add(new UserCircle("Trabalho", "3 membros ativos"));
        userCircles.setValue(circles);
    }

    private void loadNearbyFriends() {
        // Lista de contatos de confiança próximos
        List<Friend> list = new ArrayList<>();
        list.add(new Friend("Isaias", "321 m"));
        list.add(new Friend("Gilson", "550 m"));
        list.add(new Friend("Paulo", "1.2 km"));
        nearbyFriends.setValue(list);
    }

    // Class interna para os Círculos de Segurança
    public static class UserCircle {
        private final String title;
        private final String membersCount;

        public UserCircle(String title, String membersCount) {
            this.title = title;
            this.membersCount = membersCount;
        }

        public String getTitle() {
            return title;
        }

        public String getMembersCount() {
            return membersCount;
        }
    }

    // Class interna para Amigos Próximos
    public static class Friend {
        private final String name;
        private final String distance;

        public Friend(String name, String distance) {
            this.name = name;
            this.distance = distance;
        }

        public String getName() {
            return name;
        }

        public String getDistance() {
            return distance;
        }
    }
}