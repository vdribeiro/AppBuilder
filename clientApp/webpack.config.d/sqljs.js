const CopyWebpackPlugin = require('copy-webpack-plugin');
const path = require('path');

config.resolve.fallback = {
    ...config.resolve.fallback,
    fs: false,
    path: false,
    crypto: false,
};

config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            {
                from: path.resolve(__dirname, "../../node_modules/sql.js/dist/sql-wasm.wasm"),
                to: "."
            },
            {
                from: path.resolve(__dirname, "../../node_modules/sql.js/dist/sql-wasm.js"),
                to: "."
            },
            {
                from: path.resolve(__dirname, "../../node_modules/@cashapp/sqldelight-sqljs-worker/sqljs.worker.js"),
                to: "."
            }
        ]
    })
);
