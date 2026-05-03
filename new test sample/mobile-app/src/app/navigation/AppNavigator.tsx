import React, {useState} from 'react';
import {Pressable, StyleSheet, Text, View} from 'react-native';
import {AuthScreen} from '../../features/auth/screens/AuthScreen';
import {CreateUserScreen} from '../../features/users/screens/CreateUserScreen';
import {UsersListScreen} from '../../features/users/screens/UsersListScreen';

type Screen = 'auth' | 'create' | 'list';

export function AppNavigator() {
  const [screen, setScreen] = useState<Screen>('auth');

  return (
    <View style={styles.container}>
      <View style={styles.tabBar}>
        <Pressable onPress={() => setScreen('auth')} style={[styles.tab, screen === 'auth' && styles.activeTab]}>
          <Text style={[styles.tabText, screen === 'auth' && styles.activeTabText]}>Auth</Text>
        </Pressable>
        <Pressable onPress={() => setScreen('create')} style={[styles.tab, screen === 'create' && styles.activeTab]}>
          <Text style={[styles.tabText, screen === 'create' && styles.activeTabText]}>Create User</Text>
        </Pressable>
        <Pressable onPress={() => setScreen('list')} style={[styles.tab, screen === 'list' && styles.activeTab]}>
          <Text style={[styles.tabText, screen === 'list' && styles.activeTabText]}>Users List</Text>
        </Pressable>
      </View>
      <View style={styles.content}>
        {screen === 'auth' ? <AuthScreen /> : null}
        {screen === 'create' ? <CreateUserScreen onCreated={() => setScreen('list')} /> : null}
        {screen === 'list' ? <UsersListScreen /> : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  tabBar: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingTop: 16,
    gap: 12,
  },
  tab: {
    flex: 1,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: '#d7cbb6',
    paddingVertical: 12,
    alignItems: 'center',
    backgroundColor: '#fff9f1',
  },
  activeTab: {
    backgroundColor: '#1b5e55',
    borderColor: '#1b5e55',
  },
  tabText: {
    color: '#4a4238',
    fontWeight: '600',
  },
  activeTabText: {
    color: '#ffffff',
  },
  content: {
    flex: 1,
    padding: 16,
  },
});
