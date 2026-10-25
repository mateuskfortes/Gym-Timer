"""Testa conexão e instalação com ADB/Gradle simulados, sem acessar celulares."""

import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
FAKE_ADB = r'''#!/usr/bin/env python3
import json
import os
from pathlib import Path
import sys

args = sys.argv[1:]
scenario = json.loads(os.environ["ADB_SCENARIO"])
state_file = Path(os.environ["ADB_TEST_STATE"])
state = json.loads(state_file.read_text()) if state_file.exists() else {"attempts": 0, "checks": 0}
with open(os.environ["ADB_TEST_LOG"], "a") as log:
    log.write(json.dumps(args) + "\n")
if args == ["devices"]:
    print("List of devices attached")
    print(scenario.get("devices", ""))
elif args == ["mdns", "services"]:
    print("List of discovered mdns services")
    state["discoveries"] = state.get("discoveries", 0) + 1
    state_file.write_text(json.dumps(state))
    if state["discoveries"] >= scenario.get("discovery_delay", 1):
        if not scenario.get("discover_after_failure") or state["attempts"]:
            print(scenario.get("services", ""))
    sys.exit(scenario.get("mdns_exit", 0))
elif args[0] == "connect":
    outcomes = scenario.get("outcomes", [True])
    success = outcomes[min(state["attempts"], len(outcomes) - 1)]
    state.update(attempts=state["attempts"] + 1, checks=0, connected=success, endpoint=args[1])
    print("connected to " + args[1] if success else "failed to connect: Connection refused")
    state_file.write_text(json.dumps(state))
    sys.exit(0 if success else scenario.get("failure_exit", 0))
elif args[0] == "-s" and args[2:] == ["get-state"]:
    state["checks"] += 1
    state_file.write_text(json.dumps(state))
    serial = scenario.get("serial", state.get("endpoint"))
    if state.get("connected") and args[1] == serial and state["checks"] >= scenario.get("delay", 1):
        print("device")
    else:
        sys.exit(1)
'''


class RunAppTest(unittest.TestCase):
    """Só permite que a instalação ocorra após confirmar o transporte correto."""

    def run_script(self, scenario, user_input=""):
        """Isola ferramentas, entradas e arquivos temporários em cada cenário."""
        with tempfile.TemporaryDirectory() as directory:
            workspace = Path(directory)
            adb = workspace / "sdk/platform-tools/adb"
            adb.parent.mkdir(parents=True)
            adb.write_text(FAKE_ADB)
            adb.chmod(0o755)
            shutil.copy2(ROOT / "run-app.sh", workspace / "run-app.sh")
            gradle = workspace / "gradlew"
            gradle.write_text('#!/bin/sh\nprintf \'["gradle"]\\n\' >> "$ADB_TEST_LOG"\n')
            gradle.chmod(0o755)
            fake_bin = workspace / "bin"
            fake_bin.mkdir()
            (fake_bin / "sleep").write_text("#!/bin/sh\nexit 0\n")
            (fake_bin / "sleep").chmod(0o755)
            log = workspace / "calls.jsonl"
            env = dict(os.environ, ANDROID_HOME=str(workspace / "sdk"),
                       ADB_SCENARIO=json.dumps(scenario), ADB_TEST_STATE=str(workspace / "state.json"),
                       ADB_TEST_LOG=str(log), PATH=str(fake_bin) + os.pathsep + os.environ["PATH"])
            result = subprocess.run(["bash", "run-app.sh"], cwd=workspace, input=user_input,
                                    text=True, capture_output=True, env=env, timeout=15)
            calls = [json.loads(line) for line in log.read_text().splitlines()]
            return result, calls

    def assert_installed_on(self, calls, serial):
        """Confere o alvo explícito tanto na instalação quanto na abertura do app."""
        self.assertIn(["gradle"], calls)
        self.assertIn(["-s", serial, "install", "-r", "app/build/outputs/apk/dev/debug/app-dev-debug.apk"], calls)
        self.assertIn(["-s", serial, "shell", "am", "start", "-n",
                       "com.example.gymtimer.dev/com.example.gymtimer2.MainActivity"], calls)

    def test_connected_usb_skips_wifi(self):
        result, calls = self.run_script({"devices": "phone-usb\tdevice"})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assert_installed_on(calls, "phone-usb")
        self.assertFalse(any(call[0] == "connect" for call in calls))

    def test_multiple_or_unauthorized_devices_never_install(self):
        for devices in ("phone-a\tdevice\nphone-b\tdevice", "phone\tunauthorized", "phone\tno permissions"):
            with self.subTest(devices=devices):
                result, calls = self.run_script({"devices": devices})
                self.assertNotEqual(result.returncode, 0)
                self.assertNotIn(["gradle"], calls)

    def test_discovers_connection_endpoint_and_waits_for_registration(self):
        result, calls = self.run_script({"services": "phone _adb-tls-connect._tcp. 192.168.1.104:46664", "delay": 3})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assert_installed_on(calls, "192.168.1.104:46664")

    def test_waits_for_asynchronous_service_discovery(self):
        result, calls = self.run_script({"services": "phone _adb-tls-connect._tcp. 192.168.1.104:46664",
                                         "discovery_delay": 2})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assert_installed_on(calls, "192.168.1.104:46664")

    def test_resolves_mdns_serial_instead_of_assuming_ip_serial(self):
        serial = "adb-phone._adb-tls-connect._tcp"
        result, calls = self.run_script({"services": "adb-phone _adb-tls-connect._tcp. 192.168.1.104:46664", "serial": serial})
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assert_installed_on(calls, serial)

    def test_refusal_with_zero_exit_code_never_builds_or_installs(self):
        result, calls = self.run_script({"outcomes": [False]}, "\n46663\n")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("conexão TCP foi recusada", result.stderr)
        self.assertNotIn(["gradle"], calls)

    def test_can_retry_manual_connection_without_restarting(self):
        result, calls = self.run_script({"outcomes": [False, True]}, "\n46663\n\n46664\n")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assert_installed_on(calls, "192.168.1.104:46664")

    def test_rediscovers_new_port_after_manual_refusal(self):
        result, calls = self.run_script({"outcomes": [False, True], "discover_after_failure": True,
                                         "services": "phone _adb-tls-connect._tcp. 192.168.1.104:46664"}, "\n46663\n")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assert_installed_on(calls, "192.168.1.104:46664")

    def test_accepts_pasted_address_when_mdns_is_unavailable(self):
        result, calls = self.run_script({"mdns_exit": 1}, "192.168.1.104:46663\n")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assert_installed_on(calls, "192.168.1.104:46663")

    def test_pairing_advertisements_are_not_connection_targets(self):
        result, calls = self.run_script({"services": "phone _adb-tls-pairing._tcp. 192.168.1.104:12345"}, "\n46663\n")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn(["connect", "192.168.1.104:12345"], calls)
        self.assert_installed_on(calls, "192.168.1.104:46663")

    def test_invalid_addresses_and_ports_never_reach_adb_connect(self):
        for user_input in ("999.168.1.104:46663\n", "192.168.1.104:0\n", "192.168.1.104:65536\n",
                           "192.168.1.104:invalid\n", "192.168.1.104:1:2\n"):
            with self.subTest(user_input=user_input):
                result, calls = self.run_script({}, user_input)
                self.assertNotEqual(result.returncode, 0)
                self.assertFalse(any(call[0] == "connect" for call in calls))
                self.assertNotIn(["gradle"], calls)

    def test_multiple_advertised_phones_require_manual_target(self):
        services = "a _adb-tls-connect._tcp. 192.168.1.104:46663\nb _adb-tls-connect._tcp. 192.168.1.105:46664"
        result, calls = self.run_script({"services": services}, "192.168.1.105:46664\n")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn(["connect", "192.168.1.104:46663"], calls)
        self.assert_installed_on(calls, "192.168.1.105:46664")


if __name__ == "__main__":
    unittest.main()
