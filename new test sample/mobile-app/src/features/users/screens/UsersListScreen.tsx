import React, {useEffect, useState} from 'react';
import {ActivityIndicator, FlatList, Pressable, StyleSheet, Text, View} from 'react-native';
import {getUsers} from '../../../services/api/usersApi';
import {User} from '../types';

export function UsersListScreen() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadUsers() {
    setLoading(true);
    setError(null);

    try {
      const response = await getUsers();
      setUsers(response);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Failed to load users.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadUsers();
  }, []);

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <View>
          <Text style={styles.title}>Users</Text>
          <Text style={styles.subtitle}>Current records from the core application service.</Text>
        </View>
        <Pressable onPress={loadUsers} style={styles.refreshButton}>
          <Text style={styles.refreshButtonText}>Refresh</Text>
        </Pressable>
      </View>

      {loading ? (
        <View style={styles.centerState}>
          <ActivityIndicator size="large" color="#1b5e55" />
        </View>
      ) : error ? (
        <View style={styles.centerState}>
          <Text style={styles.error}>{error}</Text>
        </View>
      ) : users.length === 0 ? (
        <View style={styles.centerState}>
          <Text style={styles.empty}>No users found.</Text>
        </View>
      ) : (
        <FlatList
          data={users}
          keyExtractor={item => String(item.id)}
          contentContainerStyle={styles.list}
          renderItem={({item}) => (
            <View style={styles.card}>
              <Text style={styles.name}>{item.fullName}</Text>
              <Text style={styles.meta}>{item.email}</Text>
              <Text style={styles.meta}>{item.phoneNumber}</Text>
              <Text style={styles.badge}>{item.status}</Text>
            </View>
          )}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fffdf8',
    borderRadius: 20,
    padding: 20,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 16,
    gap: 12,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: '#12322f',
  },
  subtitle: {
    fontSize: 14,
    color: '#6a6256',
    marginTop: 4,
  },
  refreshButton: {
    backgroundColor: '#efe5d3',
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 12,
  },
  refreshButtonText: {
    color: '#4a4238',
    fontWeight: '600',
  },
  centerState: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  error: {
    color: '#b3261e',
    textAlign: 'center',
  },
  empty: {
    color: '#6a6256',
  },
  list: {
    gap: 12,
  },
  card: {
    backgroundColor: '#f6efe2',
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: '#eadfc9',
  },
  name: {
    fontSize: 18,
    fontWeight: '700',
    color: '#1a2321',
  },
  meta: {
    marginTop: 4,
    color: '#5d5448',
  },
  badge: {
    marginTop: 10,
    alignSelf: 'flex-start',
    backgroundColor: '#1b5e55',
    color: '#ffffff',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 999,
    overflow: 'hidden',
    fontWeight: '700',
  },
});
