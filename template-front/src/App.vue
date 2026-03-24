<template>
  <router-view />
</template>

<style>
:root {
  color-scheme: light;
  --app-text: #18363d;
  --app-muted: #6a8890;
  --app-border: rgba(24, 54, 61, 0.1);
  --app-panel: rgba(255, 255, 255, 0.88);
  --app-shadow: 0 24px 70px rgba(16, 56, 61, 0.12);
}

* {
  box-sizing: border-box;
}

html,
body,
#app {
  margin: 0;
  min-height: 100%;
  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
  color: var(--app-text);
  background:
      radial-gradient(circle at top left, rgba(111, 197, 194, 0.26), transparent 28%),
      radial-gradient(circle at bottom right, rgba(255, 202, 141, 0.28), transparent 24%),
      linear-gradient(145deg, #edf6f5 0%, #f9fbff 42%, #eef3f8 100%);
}

body {
  min-width: 320px;
}

a {
  color: inherit;
  text-decoration: none;
}
</style>
