let game;
let player;
let players = {};
let peer;
let currentTaggerId = null;
let conns = {};

const config = {
    type: Phaser.AUTO,
    width: 800,
    height: 600,
    scene: {
        preload, create, update
    },
    physics: { 
        default: "arcade",
        arcade: { debug: false }
    },
    parent: "game-container"
};

function preload() {
    this.load.image("player", "assets/player.png");
    this.load.image("tagger", "assets/tagger.png");
}

function create() {
    // Initialize PeerJS
    peer = new Peer();

    peer.on("open", (id) => {
        document.getElementById("status").textContent = `Your ID: ${id}`;
        document.getElementById("joinBtn").onclick = () => {
            const roomId = document.getElementById("roomId").value.trim();
            if (roomId) joinRoom(roomId);
        };
    });

    peer.on("connection", (conn) => {
        conn.on("data", (data) => handleData(data));
        conns[conn.peer] = conn;
    });

    // Create local player
    player = this.physics.add.sprite(400, 300, "player");
    player.setCollideWorldBounds(true);

    // First player becomes tagger
    if (peer._id && !currentTaggerId) {
        currentTaggerId = peer.id;
        player.setTexture("tagger");
    }

    // Movement
    this.cursors = this.input.keyboard.createCursorKeys();

    // Collision detection
    this.physics.add.collider(player, Object.values(players), (p1, p2) => {
        if (currentTaggerId === peer.id) {
            const targetId = Object.keys(players).find(id => players[id] === p2);
            if (targetId) {
                broadcastData({ type: "tag", newTaggerId: targetId });
            }
        }
    });
}

function update() {
    // Movement
    const speed = 3;
    if (this.cursors.left.isDown) player.x -= speed;
    if (this.cursors.right.isDown) player.x += speed;
    if (this.cursors.up.isDown) player.y -= speed;
    if (this.cursors.down.isDown) player.y += speed;

    // Broadcast position
    broadcastData({ 
        type: "position", 
        x: player.x, 
        y: player.y,
        peerId: peer.id
    });
}

function joinRoom(roomId) {
    const conn = peer.connect(roomId);
    conn.on("open", () => {
        document.getElementById("status").textContent = `Connected to room: ${roomId}`;
        conns[roomId] = conn;
        conn.on("data", (data) => handleData(data));
    });
}

function broadcastData(data) {
    Object.values(conns).forEach(conn => {
        conn.send(data);
    });
}

function handleData(data) {
    if (data.type === "position") {
        if (!players[data.peerId]) {
            const newPlayer = game.scene.scenes[0].physics.add.sprite(data.x, data.y, "player");
            players[data.peerId] = newPlayer;
            newPlayer.setCollideWorldBounds(true);
        } else {
            players[data.peerId].x = data.x;
            players[data.peerId].y = data.y;
        }
    } else if (data.type === "tag") {
        currentTaggerId = data.newTaggerId;
        player.setTexture(currentTaggerId === peer.id ? "tagger" : "player");
    }
}

// Start the game
game = new Phaser.Game(config);