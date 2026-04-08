/* eslint-env node */

module.exports = {
  env: {
    browser: true,
    es2020: true
  },
  extends: ['eslint:recommended', 'plugin:react/recommended', 'plugin:react/jsx-runtime', 'plugin:react-hooks/recommended', 'plugin:storybook/recommended'],
  parser: '@typescript-eslint/parser',
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module'
  },
  settings: {
    react: {
      version: '18.2'
    }
  },
  plugins: ['react-refresh', '@typescript-eslint'],
  rules: {

    // this rule is broken for typescript, disable base rule and enable typescript version
    'no-unused-vars': 'off', 
    '@typescript-eslint/no-unused-vars': ['error'], 

    'react-refresh/only-export-components': ['warn', {
      allowConstantExport: true
    }]
  }
};