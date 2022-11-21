import Foundation
import AppKit
import ArgParser

private func usage(_ errMsg: String? = nil) -> Never {
    fputs("Usage: asciipic [options] filename\n\n", stderr)
    fputs(parser.argumentHelpText(), stderr)
    fputs("\n\n", stderr)
    if let msg = errMsg {
        fputs("Error: \(msg)\n",stderr)
    }
    exit(1)
}

let aspect = BasicParam(names: ["aspectRatio","a"], initial: 1.5, help: "the aspect ratio of your text font (default: 1.5)")
let width = BasicParam(names: ["width","w"], initial: 72, help: "the desired width of the output (default: 72)")
let invert = FlagParam(names: ["invert"], help: "invert the brightness (can make some images look nicer)")
let help = FlagParam(names: ["help"], help: "prints this help text")
let parser = ArgParser(aspect,width,invert,help)
let fname : String

do {
    let extras = try parser.parseArgs(CommandLine.arguments.dropFirst())
    if help.value || extras.count != 1 { usage() }
    fname = extras[0]
} catch ArgumentErrors.invalidArgument(desc: let msg) {
    usage(msg)
} catch {
    usage("Error while parsing args!")
}

// ok time to process the input...
private func brightness(rgba: Int32) -> Double {
    let r  = Double(rgba & 0xff) * 0.2126,
        g  = Double((rgba & 0xff00) >> 8) * 0.7152,
        b  = Double((rgba & 0xff0000) >> 16) * 0.0722
    let total = r + g + b
    return invert.value ? min(256.0 - total,255.0) : total
}

let asciiTable : [Character] = [ "#", "A", "@", "%", "$", "+", "=", "*", ":", ",", ".", " " ]
private func brightChar(_ b: Double) -> Character { asciiTable[Int(b * Double(asciiTable.count) / 256.0)] }

guard let nsi = NSImage(contentsOfFile: fname), !nsi.representations.isEmpty else {
    usage("cannot load image from <\(fname)>!")
}

let iw: Double = Double(nsi.representations[0].pixelsWide), ih: Double = Double(nsi.representations[0].pixelsHigh)
let height = Int(Double(width.value) / aspect.value / iw * ih)

let imageRect = NSMakeRect(0, 0, CGFloat(width.value), CGFloat(height))

// Create a context to hold the image data
guard let colorSpace = CGColorSpace(name: CGColorSpace.genericRGBLinear),
      let ctx = CGContext(data: nil, width: width.value, height: height, bitsPerComponent: 8, bytesPerRow: 0, space: colorSpace, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue) else {
    usage("Cannot create colorspace or context for image!")
}

// Wrap graphics context
let gctx = NSGraphicsContext(cgContext: ctx, flipped: false)

// Make our bitmap context current and render the NSImage into it
NSGraphicsContext.current = gctx
nsi.draw(in: imageRect)

if ctx.bitsPerPixel != 32 {
    usage("bits per pixel wasn't 32!")
}
// the stride width can be different than the width of the image
let strideWidth = ctx.bytesPerRow / (ctx.bitsPerPixel / 8)

// now... loop over the image data...
if ctx.width != width.value || ctx.height != height {
   fputs("Rendered \(ctx.width) x \(ctx.height)  when we asked for  \(width.value) x \(height)\n", stderr)
}

var strbuff = "" ; strbuff.reserveCapacity((ctx.width + 1) * ctx.height)
guard var pixelRow = ctx.data?.bindMemory(to: Int32.self, capacity: strideWidth*ctx.height) else {
    usage("couldn't get pixel data!")
}

for _ in 0..<ctx.height {
    var curPixel = pixelRow
    for _ in 0..<ctx.width {
        strbuff.append(brightChar(brightness(rgba: curPixel.pointee)))
        curPixel = curPixel.successor()
    }
    pixelRow = pixelRow.advanced(by: strideWidth)
    strbuff.append("\n")
}

print(strbuff)

// Clean up
NSGraphicsContext.current = nil
// errors say this isn't needed...  CGContextRelease(ctx)
// errors say this isn't needed ... CGColorSpaceRelease(colorSpace)

