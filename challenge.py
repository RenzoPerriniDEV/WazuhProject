def extract_errors(log_data) -> list:
    return [line.strip().split('\n') for line in log_data if 'ERROR' in line.upper()]
