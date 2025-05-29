include "global_cardinality_low_up_closed.mzn";

% define set of possible directions with 0 = /, 1 = -45, 2 = 0, 3 = 45, 4 = 90
set of int: Directions = 0..4;
% define the cover
array[1..4] of int: cover = [1, 2, 3, 4];
% define allowed middle in the case of pair thickness
array[1..6, 1..2] of int: allowed_pair = [|
    1, 1 |
    2, 2 |
    3, 3 |
    4, 4 |
    1, 3 |
    3, 1 |
|];
% define allowed middle in the case of odd thickness
array[1..12, 1..3] of int: allowed_odd = [|
    1, 2, 1 |   % -45,      0,      -45
    1, 4, 1 |   % -45,      90,     -45
    2, 2, 2 |   % 0,        0,      0
    2, 4, 2 |   % 0,        90,     0
    3, 2, 3 |   % 45,       0,      45
    3, 4, 3 |   % 45,       90,     45
    4, 2, 4 |   % 90,       0,      90
    4, 4, 4 |   % 90,       90,     90
    1, 2, 3 |   % -45,      0,      45
    1, 4, 3 |   % -45,      90,     45
    3, 2, 1 |   % 45,       0,      -45
    3, 4, 1 |   % 45,       90,     -45
|];
% minimum ply percentage for every stacking sequence
float: min_ply_percentage = 0.08;

% INPUT
% define the size T of the thickest stacking sequence
int: T;
% define the number of stacking sequences
int: n;
% define number of edges
int: m;
% define a 2D array with the stacking sequences
array[1..n, 1..T] of var Directions: seq;
% define an array with pointers to the parent sequence (will be used in element constraint)
array[1..m, 1..T] of var 1..T: indexes;
% define the edges between the stacking sequences, the first element is the parent and the second is the child
array[_] of tuple(int, int): edges;
% define the number of plies in each direction (used for both the no 90 gap constraint and the max 4 consecutive plies in the same direction constraint)
array[1..n, 1..4] of int: counts;
% define an array with the thickness of each sequence
array[1..n] of int: thickness = array1d(1..n , [counts[i, 1] + counts[i, 2] + counts[i, 3] + counts[i, 4] | i in 1..n]);


% CONSTRAINTS
% MANDATORY CONSTRAINTS
% for each stacking sequence, make sure that the ply can't take the 0 value when within the thickness
constraint forall(i in 1..n, j in 1..thickness[i])
    (seq[i, j] != 0);
% for each stacking sequences, set all the variables after the thickness to 0
constraint forall(i in 1..n, j in thickness[i]+1..T)
    (seq[i, j] = 0);

% --blending rule: every child sequence must be a subset of the parent sequence. For every parent node A and child node B: bi = A[indexi]
constraint forall(i in 1..m, j in 1..thickness[edges[i].2])
    (seq[edges[i].2, j] = element(indexes[i, j], seq[edges[i].1, ..]));
% indexes must be lower than the thickness of the parent sequence
constraint forall(i in 1..m, j in 1..thickness[edges[i].2])
    (indexes[i, j] <= thickness[edges[i].1]);

% --no ply-crossing: indexi < indexi+1
constraint forall(i in 1..m, j in 1..thickness[edges[i].2]-1)
    (indexes[i, j] < indexes[i, j+1]);

% --surface plies must be continuous
constraint forall(i in 1..m)
    (indexes[i, 1] = 1);
constraint forall(i in 1..m)
    (indexes[i, thickness[edges[i].2]] = thickness[edges[i].1]);

% -- B4 max 4 dropped plies: |indexi - indexi+1| <= 4
constraint forall(i in 1..m, j in 1..thickness[edges[i].2]-1)
    ((indexes[i, j+1] - indexes[i, j]) <= 4);

% OPTIONAL CONSTRAINTS
% --table constraint containing the no 90 gap contraint and the max 4 consecutive plies in the same direction constraint
constraint forall (i in 1..n, j in 1..thickness[i]- 1) (
    if j < floor(thickness[i]/2) \/ j > ceil(thickness[i]/2) then
        abs(seq[i, j] - seq[i, j+1]) != 2
    endif
);

% constraint max 4 consecutive angles with same value --> seq[i, j] != seq[i, j+1] or seq[i, j] != seq[i, j+2] or seq[i, j] != seq[i, j+3] or seq[i, j] != seq[i, j+4]
constraint forall(i in 1..n, j in 1..thickness[i]-4) (
    seq[i, j] != seq[i, j+1] \/ seq[i, j] != seq[i, j+2] \/ seq[i, j] != seq[i, j+3] \/ seq[i, j] != seq[i, j+4]
);

% --symmetric sequences: for every node A with thickness t: ai = at-i (except for the middle part, see next constraint)
constraint forall(i in 1..n, j in 1..floor(thickness[i]/2)-1)
    (seq[i, j] = seq[i, thickness[i]-j+1]);

% --allow dysymmetry in the middle (cf. paper & constraints)
constraint forall(i in 1..n) (
    if thickness[i] mod 2 == 0 then
        table([seq[i, floor(thickness[i]/2)], seq[i, floor(thickness[i]/2)+1]], allowed_pair)
    else
        table([seq[i, floor(thickness[i]/2)], seq[i, floor(thickness[i]/2)+1], seq[i, floor(thickness[i]/2)+2]], allowed_odd)
    endif
);

% D1 -- a given number of plies in each direction is given
constraint forall(i in 1..n)
    (global_cardinality_low_up(
        seq[i, ..],
        cover,
        [floor((counts[i, 1] + counts[i, 3]) / 2), counts[i, 2], floor((counts[i, 1] + counts[i, 3]) / 2), counts[i, 4]],
        [ceil((counts[i, 1] + counts[i, 3]) / 2), counts[i, 2], ceil((counts[i, 1] + counts[i, 3]) / 2), counts[i, 4]]
    ));

% -- D5 surface plies can only take 45/-45 angles
constraint forall(i in 1..n)
    (seq[i, 1] != 0 /\ seq[i, 1] != 2 /\ seq[i, 1] != 4);
constraint forall(i in 1..n)
    (seq[i, thickness[i]] != 0 /\ seq[i, thickness[i]] != 2 /\ seq[i, thickness[i]] != 4);


%% OPTIMIZATION FUN
array[1..n, 1..T-3] of var bool: ply_count = array2d(1..n, 1..T-3, [seq[i, j] == seq[i, j+1] /\ seq[i, j] == seq[i, j+2] /\ seq[i, j] == seq[i, j+3] | i in 1..n, j in 1..T-3]);

var int: objective = sum(i in 1..n, j in 1..thickness[i]-3) (ply_count[i, j]);

solve minimize objective;

%output ["seq = ",show(seq), "\n",
%        ", objective = ",show(objective), "\n",
%        ", counts = ", show(counts), "\n",
%        ", ply_count = ",show(ply_count), "\n"];
