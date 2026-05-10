import AVFoundation
import AppKit
import Foundation

let videoURL = URL(fileURLWithPath: "/Users/nikitatukan/Documents/Playground/da.mp4")
let outputDirectory = URL(fileURLWithPath: "/Users/nikitatukan/Documents/Playground/video_frames", isDirectory: true)

try? FileManager.default.createDirectory(at: outputDirectory, withIntermediateDirectories: true)

let asset = AVURLAsset(url: videoURL)
let durationSeconds = CMTimeGetSeconds(asset.duration)
let generator = AVAssetImageGenerator(asset: asset)
generator.appliesPreferredTrackTransform = true
generator.maximumSize = CGSize(width: 1400, height: 1400)

let timePoints = stride(from: 0.0, through: max(durationSeconds - 0.1, 0.0), by: max(durationSeconds / 6.0, 1.0)).map {
    CMTime(seconds: $0, preferredTimescale: 600)
}

for (index, time) in timePoints.enumerated() {
    do {
        let imageRef = try generator.copyCGImage(at: time, actualTime: nil)
        let image = NSImage(cgImage: imageRef, size: .zero)
        guard let tiffData = image.tiffRepresentation,
              let bitmap = NSBitmapImageRep(data: tiffData),
              let pngData = bitmap.representation(using: .png, properties: [:]) else {
            continue
        }

        let seconds = String(format: "%.2f", CMTimeGetSeconds(time))
        let fileURL = outputDirectory.appendingPathComponent("frame_\(index)_\(seconds).png")
        try pngData.write(to: fileURL)
        print(fileURL.path)
    } catch {
        print("Failed at \(CMTimeGetSeconds(time)): \(error)")
    }
}
