import React from 'react';
import {SafeAreaView, StatusBar, StyleSheet} from 'react-native';
import {AppNavigator} from './navigation/AppNavigator';

export function AppRoot() {
  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <AppNavigator />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f4f4ef',
  },
});
