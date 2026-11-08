"""Runs every own test of this repository.   python tests/student/run_tests.py   (from the repository root)"""
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
S = ROOT / "tests" / "student"
SEP = ":" if sys.platform != "win32" else ";"


def step(title, cmd):
    print(f"\n=== {title}")
    code = subprocess.run(cmd, cwd=ROOT).returncode
    print(f"=== {title}: {'OK' if code == 0 else 'FAILED'}")
    return code


def main():
    build = ROOT / ".build"
    (build / "Java").mkdir(parents=True, exist_ok=True)
    (build / "tests").mkdir(parents=True, exist_ok=True)
    srcs = [str(p) for p in sorted((ROOT / "Java" / "source").glob("*.java"))]
    codes = [step("compile solution", ["javac", "--release", "17", "-encoding", "UTF-8", "-d", str(build / "Java"), *srcs])]
    codes.append(step("compile Tests.java", ["javac", "--release", "17", "-encoding", "UTF-8", "-cp", str(build / "Java"),
                                            "-d", str(build / "tests"), str(S / "java" / "Tests.java")]))
    codes.append(step("oracle tests (Tests.java)", ["java", "-Xmx384m", "-cp", str(build / "Java") + SEP + str(build / "tests"), "Tests"]))
    codes.append(step("hand-written cases", [sys.executable, str(S / "check_cases.py")]))
    codes.append(step("maximum-size batch", [sys.executable, str(S / "check_large.py")]))
    print("\nALL OWN TESTS PASSED" if not any(codes) else "\nSOME OWN TESTS FAILED")
    return 1 if any(codes) else 0


if __name__ == "__main__":
    sys.exit(main())
