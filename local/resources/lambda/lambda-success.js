exports.handler = async function (event) {
    console.log("EVENT: \n" + JSON.stringify(event, null, 2));
    return event;
}