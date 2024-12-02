const path = require('path');

config.webpack.resolve.alias = {
    ...config.webpack.resolve.alias,
    './skiko.mjs$': require.resolve('skiko-js-wasm-runtime/skiko.mjs'),
};
