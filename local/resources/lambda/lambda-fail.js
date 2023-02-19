exports.handler = async function (event) {
    throw new Error("Test error on event: " + JSON.stringify(event));
}