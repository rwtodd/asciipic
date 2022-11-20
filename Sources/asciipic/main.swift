import Foundation
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
let help = FlagParam(names: ["help"], help: "prints this help text")
let parser = ArgParser(aspect,width,help)
var fname : String

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

print("processing \(fname) with AR \(aspect.value) and width \(width.value)")
