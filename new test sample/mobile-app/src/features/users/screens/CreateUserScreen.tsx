import React, {useState} from 'react';
import {Alert, Pressable, StyleSheet, Text, TextInput, View} from 'react-native';
import {createUser} from '../../../services/api/usersApi';

type Props = {
  onCreated: () => void;
};

export function CreateUserScreen({onCreated}: Props) {
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit() {
    setSubmitting(true);
    setError(null);

    try {
      await createUser({fullName, email, phoneNumber});
      setFullName('');
      setEmail('');
      setPhoneNumber('');
      Alert.alert('Success', 'User created successfully.');
      onCreated();
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Failed to create user.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Phase 1 User Creation</Text>
      <Text style={styles.subtitle}>Create a user through the core application service.</Text>

      <TextInput
        placeholder="Full name"
        placeholderTextColor="#8a7b69"
        style={styles.input}
        value={fullName}
        onChangeText={setFullName}
      />
      <TextInput
        placeholder="Email"
        placeholderTextColor="#8a7b69"
        style={styles.input}
        value={email}
        onChangeText={setEmail}
        autoCapitalize="none"
        keyboardType="email-address"
      />
      <TextInput
        placeholder="Phone number"
        placeholderTextColor="#8a7b69"
        style={styles.input}
        value={phoneNumber}
        onChangeText={setPhoneNumber}
        keyboardType="phone-pad"
      />

      {error ? <Text style={styles.error}>{error}</Text> : null}

      <Pressable style={[styles.button, submitting && styles.buttonDisabled]} onPress={handleSubmit} disabled={submitting}>
        <Text style={styles.buttonText}>{submitting ? 'Creating...' : 'Create User'}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    gap: 12,
    backgroundColor: '#fffdf8',
    borderRadius: 20,
    padding: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
    color: '#12322f',
  },
  subtitle: {
    fontSize: 14,
    color: '#6a6256',
    marginBottom: 8,
  },
  input: {
    backgroundColor: '#f5efe3',
    borderRadius: 14,
    paddingHorizontal: 14,
    paddingVertical: 12,
    color: '#1f1f1f',
    borderWidth: 1,
    borderColor: '#eadfc9',
  },
  error: {
    color: '#b3261e',
    fontSize: 13,
  },
  button: {
    marginTop: 8,
    backgroundColor: '#1b5e55',
    paddingVertical: 14,
    borderRadius: 14,
    alignItems: 'center',
  },
  buttonDisabled: {
    opacity: 0.7,
  },
  buttonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: '700',
  },
});
