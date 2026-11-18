// Safari 27.2: Aufrufe der Grafik-Bibliothek aus der Endaufruf-Stellung nehmen (siehe web/webpack/skiko-no-tail-call.cjs)
config.module = config.module || {}; config.module.rules = config.module.rules || [];
config.module.rules.push({ test: /skiko\.mjs$/, enforce: 'pre', use: [{ loader: require('path').resolve(__dirname, '../../../../web/webpack/skiko-no-tail-call.cjs') }] });
