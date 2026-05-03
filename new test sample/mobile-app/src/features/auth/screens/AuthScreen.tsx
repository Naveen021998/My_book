import React, {useState} from 'react';
import {Pressable, StyleSheet, Text, TextInput, View} from 'react-native';
import {login, register} from '../../../services/api/authApi';
import {AuthTokens} from '../types';

type Mode = 'login' | 'register';

export function AuthScreen() {
  const [mode, setMode] = useState<Mode>('register');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tokens, setTokens] = useState<AuthTokens | null>(null);

  async function handleSubmit() {
    setLoading(true);
    setError(null);

    try {
      const response = mode === 'register'
        ? await register({fullName, email, password})
        : await login({email, password});
      setTokens(response);
      if (mode === 'register') {
        setFullName('');
      }
      setPassword('');
    } catch (submitError) {
      setError(submitError instanceof Error ? submitError.message : 'Authentication request failed.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <View style={styles.container}>
      <View style={styles.modeRow}>
        <Pressable style={[styles.modeButton, mode === 'register' && styles.modeButtonActive]} onPress={() => setMode('register')}>
          <Text style={[styles.modeText, mode === 'register' && styles.modeTextActive]}>Register</Text>
        </Pressable>
        <Pressable style={[styles.modeButton, mode === 'login' && styles.modeButtonActive]} onPress={() => setMode('login')}>
          <Text style={[styles.modeText, mode === 'login' && styles.modeTextActive]}>Login</Text>
        </Pressable>
      </View>

      <Text style={styles.title}>Authorization Service</Text>
      <Text style={styles.subtitle}>Register a user or sign in against the production auth module.</Text>

      {mode === 'register' ? (
        <TextInput
          placeholder="Full name"
          placeholderTextColor="#8a7b69"
          style={styles.input}
          value={fullName}
          onChangeText={setFullName}
        />
      ) : null}

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
        placeholder="Password"
        placeholderTextColor="#8a7b69"
        style={styles.input}
        value={password}
        onChangeText={setPassword}
        secureTextEntry
      />

      {error ? <Text style={styles.error}>{error}</Text> : null}

      <Pressable style={[styles.button, loading && styles.buttonDisabled]} onPress={handleSubmit} disabled={loading}>
        <Text style={styles.buttonText}>{loading ? 'Submitting...' : mode === 'register' ? 'Create Account' : 'Login'}</Text>
      </Pressable>

      {tokens ? (
        <View style={styles.resultCard}>
          <Text style={styles.resultTitle}>Active Session</Text>
          <Text style={styles.resultText}>{tokens.user.fullName}</Text>
          <Text style={styles.resultText}>{tokens.user.email}</Text>
          <Text style={styles.resultMono}>access: {tokens.accessToken.slice(0, 16)}...</Text>
          <Text style={styles.resultMono}>refresh: {tokens.refreshToken.slice(0, 16)}...</Text>
        </View>
      ) : null}
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
  modeRow: {
    flexDirection: 'row',
    gap: 12,
  },
  modeButton: {
    flex: 1,
    borderRadius: 14,
    paddingVertical: 10,
    borderWidth: 1,
    borderColor: '#d7cbb6',
    backgroundColor: '#fff9f1',
    alignItems: 'center',
  },
  modeButtonActive: {
    backgroundColor: '#9f3d2e',
    borderColor: '#9f3d2e',
  },
  modeText: {
    color: '#4a4238',
    fontWeight: '600',
  },
  modeTextActive: {
    color: '#ffffff',
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
    backgroundColor: '#9f3d2e',
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
  resultCard: {
    marginTop: 8,
    backgroundColor: '#f6efe2',
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: '#eadfc9',
    gap: 6,
  },
  resultTitle: {
    fontWeight: '700',
    color: '#1a2321',
  },
  resultText: {
    color: '#5d5448',
  },
  resultMono: {
    color: '#3b3b3b',
    fontFamily: 'monospace',
    fontSize: 12,
  },
});
