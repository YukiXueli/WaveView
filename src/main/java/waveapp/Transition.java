//
// Copyright 2011-2012 Jeff Bush
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package waveapp;

///
/// Represents a change in a single net (which may have multiple bits)
/// The is derived from a BitVector and contains the value at and after
/// the given timestamp.
///

public class Transition extends BitVector {
    public long getTimestamp() {
        return fTimestamp;
    }

    void setTimestamp(long timestamp) {
        fTimestamp = timestamp;
    }

    private long fTimestamp;
}