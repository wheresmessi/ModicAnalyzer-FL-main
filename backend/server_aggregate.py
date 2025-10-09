import numpy as np
from pathlib import Path
from typing import List


def load_npz(path: str):
    """Load a .npz and return a dict of arrays."""
    with np.load(path, allow_pickle=False) as data:
        return {k: data[k] for k in data.files}


def aggregate_npz_files(input_paths: List[str], output_path: str):
    """Aggregate multiple .npz files by averaging arrays with matching keys.

    Each file should be a .npz with the same set of keys and matching shapes.
    Saves the averaged arrays into output_path (.npz).
    """
    if not input_paths:
        raise ValueError("No input files provided")

    loaded = [load_npz(p) for p in input_paths]

    # ensure keys match
    keys = list(loaded[0].keys())
    for d in loaded[1:]:
        if list(d.keys()) != keys:
            raise ValueError("Mismatch in keys across uploaded files")

    averaged = {}
    for k in keys:
        arrays = [d[k].astype(np.float64) for d in loaded]
        # stack and mean
        stacked = np.stack(arrays, axis=0)
        averaged[k] = np.mean(stacked, axis=0)

    # save
    np.savez_compressed(output_path, **averaged)

    return output_path
