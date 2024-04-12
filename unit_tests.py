import unittest
from challenge import extract_errors


class TestExtractErrors(unittest.TestCase):

    def test_no_errors(self):
        log_data = [
            "INFO: This is an information message.",
            "WARNING: This is a warning message."
        ]
        self.assertEqual(extract_errors(log_data), [])

    def test_single_error(self):
        log_data = [
            "ERROR: This is an error message."
        ]
        self.assertEqual(extract_errors(log_data), [['ERROR: This is an error message.']])

    def test_multiple_errors(self):
        log_data = [
            "INFO: This is an information message.",
            "ERROR: First error message.",
            "WARNING: This is a warning message.",
            "ERROR: Second error message."
        ]
        self.assertEqual(extract_errors(log_data), [['ERROR: First error message.'],
                                                    ['ERROR: Second error message.']])

    def test_errors_case_insensitive(self):
        log_data = [
            "error: This is an error message.",
            "Error: This is another error message."
        ]
        self.assertEqual(extract_errors(log_data), [['error: This is an error message.'],
                                                    ['Error: This is another error message.']])

    def test_empty_input(self):
        log_data = []
        self.assertEqual(extract_errors(log_data), [])


if __name__ == '__main__':
    unittest.main()
