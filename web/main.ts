const frameElement = document.getElementById('frame') as HTMLImageElement;
const fpsElement = document.getElementById('fps');
const resolutionElement = document.getElementById('resolution');

// A 1x1 transparent GIF to use as a placeholder
const placeholderBase64 = 'R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7';

let frameCount = 0;
const initialWidth = 640;
const initialHeight = 480;

// Set initial placeholder image and resolution text
if (frameElement) {
    frameElement.src = `data:image/gif;base64,${placeholderBase64}`;
}
if (resolutionElement) {
    resolutionElement.textContent = `Resolution: ${initialWidth}x${initialHeight}`;
}

// This function updates the FPS display once every second
setInterval(() => {
    if (fpsElement) {
        fpsElement.textContent = `FPS: ${frameCount}`;
    }
    frameCount = 0; // Reset the counter for the next second
}, 1000);

/**
 * This function should be called when a new frame is received (e.g., from a WebSocket).
 * It updates the image source and resolution display, and increments the frame counter.
 *
 * @param base64Image The base64 encoded image data (e.g., JPEG).
 * @param newWidth The width of the new image.
 * @param newHeight The height of the new image.
 */
function onNewFrame(base64Image: string, newWidth: number, newHeight: number) {
    if (frameElement) {
        frameElement.src = `data:image/jpeg;base64,${base64Image}`;
    }
    if (resolutionElement) {
        resolutionElement.textContent = `Resolution: ${newWidth}x${newHeight}`;
    }
    frameCount++;
}

// --- Example Usage ---
// To test this, you could simulate receiving frames by calling this from the browser console:
// setInterval(() => onNewFrame(placeholderBase64, 1280, 720), 100);
