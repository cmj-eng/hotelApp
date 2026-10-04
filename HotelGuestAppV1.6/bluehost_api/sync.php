<?php
// ══════════════════════════════════════════════════════════════════════════════
//  sync.php — Nduyaka Booking Sync API
//  Upload this file to: /public_html/nduyaka-staff/api/sync.php
//
//  HOW TO SET UP:
//  1. Change the API_KEY below to any secret phrase you choose
//     (e.g. "NduYaka2026!Secret" — make it long and unique)
//  2. Upload this file to Bluehost at /nduyaka-staff/api/sync.php
//  3. In the JavaFX app → Settings → Server Sync:
//     - URL:  https://www.nduyaka.com/nduyaka-staff/api/sync.php
//     - Key:  (same secret phrase you chose above)
//  4. Click "Test & Sync Now" — you should see ✅ in the status bar
// ══════════════════════════════════════════════════════════════════════════════

// ─── EDIT: Change this to your own secret key ────────────────────────────────
define('API_KEY', 'CHANGE_ME_TO_YOUR_OWN_SECRET_KEY');
// ─────────────────────────────────────────────────────────────────────────────

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, X-Api-Key');

// Handle preflight OPTIONS request (CORS)
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') { http_response_code(200); exit; }

// ── Authenticate ──────────────────────────────────────────────────────────────
$key = $_SERVER['HTTP_X_API_KEY'] ?? '';
if ($key !== API_KEY) {
    http_response_code(403);
    echo json_encode(['success' => false, 'error' => 'Invalid API key']);
    exit;
}

$guestsFile = __DIR__ . '/../data/guests.json';

// ── PULL: GET ?action=pull ────────────────────────────────────────────────────
if ($_SERVER['REQUEST_METHOD'] === 'GET' && ($_GET['action'] ?? '') === 'pull') {
    if (!file_exists($guestsFile)) {
        echo json_encode(['success' => true, 'guests' => []]);
        exit;
    }
    $data = file_get_contents($guestsFile);
    $guests = json_decode($data, true) ?? [];
    echo json_encode(['success' => true, 'guests' => $guests]);
    exit;
}

// ── PUSH: POST (body = JSON array of guests) ──────────────────────────────────
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $body = file_get_contents('php://input');
    if (empty($body)) {
        http_response_code(400);
        echo json_encode(['success' => false, 'error' => 'Empty body']);
        exit;
    }
    // Validate it's a JSON array
    $decoded = json_decode($body);
    if (json_last_error() !== JSON_ERROR_NONE || !is_array($decoded)) {
        http_response_code(400);
        echo json_encode(['success' => false, 'error' => 'Invalid JSON']);
        exit;
    }
    // Write to guests.json
    $result = file_put_contents($guestsFile, $body, LOCK_EX);
    if ($result === false) {
        http_response_code(500);
        echo json_encode(['success' => false, 'error' => 'Could not write file']);
        exit;
    }
    echo json_encode(['success' => true, 'written' => strlen($body)]);
    exit;
}

http_response_code(405);
echo json_encode(['success' => false, 'error' => 'Method not allowed']);
